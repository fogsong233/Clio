package tech.fogsong.storage

import net.mamoe.mirai.console.plugin.PluginFileExtensions
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.*
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

const val CACHE_IMAGE_DIRECTORY = "cache_image"
const val DOCUMENT_DIRECTORY = "docs"
const val CONFIG_NAME = "config.properties"
lateinit var basePath: File
val props: Properties = Properties()

object ConfigKey {

    // 开启贪婪模式将会默认获取不加标记的内容
    const val GREED_MODE = "greed_mode"

    // 使用,划分的群号
    const val ENABLE_GROUP_ID_LIST = "enable_group_ids"

    // 关键符号
    const val KEY_SYMBOL = "key"
    const val DOC_NUMBERS = "doc_numbers"
    const val BOT_ID = "bot_qq_id"
}

object StorageConfig {

    private var docNum: Int? = null
    fun init(pluginFileExtensions: PluginFileExtensions) =
        pluginFileExtensions.apply {
            val path = resolveDataPath("/")
            path.resolve(CACHE_IMAGE_DIRECTORY).let {
                if (!it.exists()) {
                    it.createDirectory()
                }
            }
            path.resolve(DOCUMENT_DIRECTORY).let {
                if (!it.exists()) {
                    it.createDirectory()
                }
            }
            // 加载配置
            resolveConfigFile(CONFIG_NAME).let {
                if (!it.exists()) {
                    with(ConfigKey) {
                        props.setProperty(DOC_NUMBERS, "0")
                        props.setProperty(GREED_MODE, "true")
                        props.setProperty(ENABLE_GROUP_ID_LIST, "")
                        props.setProperty(KEY_SYMBOL, "#")
                        props.setProperty(BOT_ID, "-1")
                    }
                    it.createNewFile()
                    props.store(FileWriter(resolveConfigFile(CONFIG_NAME), StandardCharsets.UTF_8), "SAVE")
                } else {
                    props.load(FileReader(resolveConfigFile(CONFIG_NAME), StandardCharsets.UTF_8))
                }
            }
            basePath = resolveConfigFile("/")
            docNum = props.getProperty(ConfigKey.DOC_NUMBERS).toInt()
        }

    fun save(pluginFileExtensions: PluginFileExtensions) =
        pluginFileExtensions.apply {
            props.store(FileWriter(resolveConfigFile(CONFIG_NAME), StandardCharsets.UTF_8), "SAVE")
        }

    fun getCacheImagePath(fileName: String): File {
        return basePath.resolve(CACHE_IMAGE_DIRECTORY).resolve(fileName)
    }

    fun getDocPath(fileName: String, groupId: Long): File {
        return basePath.resolve(DOCUMENT_DIRECTORY).resolve("$groupId").resolve(fileName)
    }

    fun genDocName(overview: String?, groupId: Long): String {
        val dateStr = LocalDate.now().toString()
        docNum = docNum!!.plus(1)
        return "[${docNum}][$dateStr]${overview ?: "问题"}.docx"
    }
}
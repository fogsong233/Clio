package tech.fogsong

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import tech.fogsong.bot.startBot
import tech.fogsong.storage.StorageConfig

object Clio : KotlinPlugin(
    JvmPluginDescription(
        id = "tech.fogsong.qqlogger.clio",
        name = "Clio",
        version = "0.1.0",
    ) {

        author("fogsong")
    }
) {
    override fun onEnable() {
        logger.pluginLog { "Plugin loaded" }
        StorageConfig.init(this)
        CoroutineScope(coroutineContext).launch {
            startBot()
        }
    }

    override fun onDisable() {
        super.onDisable()
        StorageConfig.save(this)
    }
}
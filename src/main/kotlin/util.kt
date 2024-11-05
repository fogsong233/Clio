package tech.fogsong

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.MessageContent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import tech.fogsong.docx.ChatMsgContent
import tech.fogsong.docx.DocMsgItem
import tech.fogsong.storage.ConfigKey
import tech.fogsong.storage.StorageConfig
import tech.fogsong.storage.props
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

fun MiraiLogger.pluginLog(msg: () -> String?) {
    info { "[DiscusstionLogger]${msg.invoke()}" }
}

fun EventChannel<GroupMessageEvent>.filterInEnableGroup() =
    filter { "${it.group.id}" in props.getProperty(ConfigKey.ENABLE_GROUP_ID_LIST, "") }


suspend fun GroupMessageEvent.msg2DocMsgItem() = DocMsgItem(
    qqAccountID = sender.id.toString(),
    qqName = senderName,
    callMsg = message[QuoteReply]?.let { quoteReply ->
        DocMsgItem(
            qqAccountID = source.fromId.toString(),
            qqName = source.fromId.let { group[it].toString() },
            callMsg = null,
            contentList = listOf(
                ChatMsgContent(
                    type = ChatMsgContent.MsgContentType.STRING,
                    contentOrPath = quoteReply.contentToString()
                )
            )
        )
    },
    contentList = message.filterIsInstance<MessageContent>().map { it.toDocMsgContent(group) }
)

// 完成圖片的保存
suspend fun MessageContent.toDocMsgContent(group: Group): ChatMsgContent {
    when (this) {
        is FileMessage -> { // 图片失效，只能使用文件上传
            // 先下载
            if (name.split(".").last() in listOf("jpg", "png", "jpeg")) {
                toAbsoluteFile(group)?.apply {
                    getUrl()?.download(name)
                }
            }
            return ChatMsgContent(
                type = ChatMsgContent.MsgContentType.IMAGE,
                contentOrPath = name
            )
        }

        is PlainText -> {
            return ChatMsgContent(
                type = ChatMsgContent.MsgContentType.STRING,
                contentOrPath = this.content
            )
        }
    }
    return ChatMsgContent(
        type = ChatMsgContent.MsgContentType.STRING,
        contentOrPath = "aaadwd"
    )
}

private val httpClient = HttpClient.newBuilder().build()

/**
 * @return 文件名称，包括后缀，默认存储在cache目录
 */
suspend fun String.download(
    fileName: String,
    dir: File = StorageConfig.getCacheImageDir()
) {
    withContext(Dispatchers.IO) {
        val req = HttpRequest.newBuilder(URI(this@download))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 5.1; OPPO R9tm Build/LMY47I; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.49 Mobile MQQBrowser/6.2 TBS/043128 Safari/537.36 V1_AND_SQ_7.0.0_676_YYB_D PA QQ/7.0.0.3135 NetType/4G WebP/0.3.0 Pixel/1080"
            )
            .timeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build()
        val res = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream())
        dir.mkdirs()
        res.body().use { ins ->
            dir.resolve(fileName).apply {
                if (!exists()) {
                    createNewFile()
                }
            }.outputStream().use { out ->
                ins.copyTo(out)
            }
        }
    }
}


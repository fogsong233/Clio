package tech.fogsong.bot

import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.NudgeEvent
import net.mamoe.mirai.utils.BotConfiguration
import tech.fogsong.docx.DocFlow
import tech.fogsong.filterNotInEnableGroup
import tech.fogsong.pluginLog
import tech.fogsong.storage.ConfigKey
import tech.fogsong.storage.props

suspend fun KotlinPlugin.startBot() {
    val botId = props.getProperty(ConfigKey.BOT_ID).toLong()
    if (botId <= 0) {
        logger.pluginLog { "没有设置机器人qq号！" }
        return
    }
    val bot = BotFactory.newBot(
        botId,
        BotAuthorization.byQRCode()
    ) {
        protocol = BotConfiguration.MiraiProtocol.MACOS
        fileBasedDeviceInfo("device.json")
    }
    bot.login()
    if (bot.isOnline) {
        logger.pluginLog { "success log in ${bot.id}----" }
        logger.pluginLog {
            var msg = ""
            props.getProperty(ConfigKey.ENABLE_GROUP_ID_LIST, "").split(",").forEach {
                msg += "enable in  $it\n"
            }
            msg
        }
    }
    // 启动完毕

    // 构建doc
    val openingDocs = mutableListOf<DocFlow>()

    bot.eventChannel
        .filterIsInstance<GroupMessageEvent>()
        .filterNotInEnableGroup()
        .subscribeAlways<GroupMessageEvent> {
            logger.pluginLog { it.message.contentToString() }
            subject.sendMessage("可莉不知道捏")
        }
}
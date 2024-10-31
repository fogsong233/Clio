package tech.fogsong

import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.info
import tech.fogsong.storage.ConfigKey
import tech.fogsong.storage.props

fun MiraiLogger.pluginLog(msg: () -> String?) {
    info { "[DiscusstionLogger]${msg.invoke()}" }
}

fun EventChannel<GroupMessageEvent>.filterNotInEnableGroup() =
    filter { "${it.group.id}" in props.getProperty(ConfigKey.ENABLE_GROUP_ID_LIST, "") }
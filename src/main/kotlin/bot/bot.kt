package tech.fogsong.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.whileSelectMessages
import net.mamoe.mirai.message.data.FileMessage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageContent
import tech.fogsong.docx.DocFlow
import tech.fogsong.docx.GroupState
import tech.fogsong.download
import tech.fogsong.filterInEnableGroup
import tech.fogsong.msg2DocMsgItem
import tech.fogsong.pluginLog
import tech.fogsong.storage.ConfigKey
import tech.fogsong.storage.StorageConfig
import tech.fogsong.storage.props

private var stateStack = mutableMapOf<Long, MutableList<GroupState>>()
suspend fun KotlinPlugin.startBot() {

    val botId = props.getProperty(ConfigKey.BOT_ID).toLong()
    if (botId <= 0) {
        logger.pluginLog { "没有设置机器人qq号！" }
        return
    }

    GlobalEventChannel.subscribeOnce<BotOnlineEvent> {
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
    val openingDocs = mutableMapOf<Long, MutableList<DocFlow>>()
    val groupIsOpeningQuestion = mutableMapOf<Long, Boolean>()

    props.getProperty(ConfigKey.ENABLE_GROUP_ID_LIST, "")
        .split(",")
        .forEach {
            val groupId = it.toLong()
            openingDocs[groupId] = mutableListOf()
            groupIsOpeningQuestion[groupId] = false
            stateStack[groupId] = mutableListOf(GroupState.NONE)
        }


    // 处理消息
    suspend fun GroupMessageEvent.questionProcess() {
        // 解析消息，记录消息
        // 如果开始记录
        val docList = openingDocs[group.id]!!
        val key = props.getProperty(ConfigKey.KEY_SYMBOL, "#")
        val startDoubleKey = key.repeat(2)
        val startMultipleKey = key.repeat(3)
        val rawMsg = message.contentToString()
        when {
            rawMsg.startsWith(startMultipleKey) -> {
                stateStack[group.id]!!.add(GroupState.QUESTION)
                subject.sendMessage("开始录制")
                val beforeTime = System.currentTimeMillis()
                whileSelectMessages {
                    val newDocFlow = DocFlow.create()
                    startMultipleKey {
//                            docList.add(newDocFlow)
                        subject.sendMessage("记录完毕")
                        docList.add(newDocFlow)
                        stateStack[group.id]!!.let { states ->
                            states.removeLast()
                            if (states.last() != GroupState.DISCUSSION) {
                                states.add(GroupState.DISCUSSION)
                            }
                        }
                        false
                    }
                    default {
                        // 设置超时
                        if (System.currentTimeMillis() - beforeTime >= 200000) {
                            subject.sendMessage("问题超时，自动关闭")
                            stateStack[group.id]!!.removeLast()
                            return@default false
                        }
                        newDocFlow.addQuestionMsg(this.msg2DocMsgItem())
                        true
                    }
                }
            }

            rawMsg.startsWith(startDoubleKey) -> {
                val newDocFlow = DocFlow.create()
                    .addQuestionMsg(this.msg2DocMsgItem())
                    .let { docList.add(it) }
                subject.sendMessage("开始记录")
                stateStack[group.id]!!.let { states ->
                    if (states.last() != GroupState.DISCUSSION) {
                        states.add(GroupState.DISCUSSION)
                    }
                }
            }
        }
    }

    suspend fun GroupMessageEvent.discussionProcess() {
        val rawMsg = message.contentToString()
        val key = props.getProperty(ConfigKey.KEY_SYMBOL, "#")
        val docList = openingDocs[group.id]!!
        val regex4End = Regex("^${key}end\\d+$")
        if (regex4End.matches(rawMsg)) {
            val index = rawMsg.split("d").last().toInt() - 1
            if (index < 0 || index >= docList.size) {
                subject.sendMessage("非法问题，请重试")
                return
            }
            stateStack[group.id]!!.removeLast()
            val doc = docList.removeAt(index)
            withContext(Dispatchers.IO) {
                val xwpfDocument = doc.build(group.id)
                StorageConfig
                    .getDocPath(StorageConfig.genDocName(null, group.id), group.id).apply {
                        createNewFile()
                    }
                    .outputStream().use {
                        xwpfDocument.write(it)
                    }
                subject.sendMessage("保存成功")
            }


            return
        }
        val regex = Regex("${key}\\d+$")
        if (regex.matches(rawMsg)) { // 特定匹配
            val index = rawMsg.split(key).last().toInt() - 1
            if (index < 0 || index >= docList.size) {
                subject.sendMessage("非法问题，请重试")
                return
            }
            docList[index].addDiscussionMsg(this.msg2DocMsgItem())
            return
        }

        if (props.getProperty(ConfigKey.GREED_MODE, "true") == "true"
            || message.filterIsInstance<Image>().isNotEmpty() // 图片默认也上传
        ) {
            // 所有数据都获取
            val msgItem = this.msg2DocMsgItem()
            docList.forEach {
                it.addDiscussionMsg(msgItem)
            }
            return
        }
    }

    GlobalEventChannel
        .filter { it is BotEvent && it.bot.id == botId }
        .filterIsInstance<GroupMessageEvent>()
        .filterInEnableGroup()
        .apply {
            println("now state: ${stateStack.toString()}")
            // questions
            subscribeAlways<GroupMessageEvent> {
                if (stateStack[group.id]!!.last()
                    in listOf(GroupState.DISCUSSION, GroupState.NONE)
                ) {
                    questionProcess()
                }
            }
            // discussion
            subscribeAlways<GroupMessageEvent> {
                if (stateStack[group.id]!!.last() == GroupState.DISCUSSION) {
                    discussionProcess()
                }
            }

            // 指令
            subscribeAlways<GroupMessageEvent> {
                val rawMsg = message.contentToString()
                if (rawMsg == props.getProperty(ConfigKey.KEY_SYMBOL, "#") + "EndAll") {
                    // 清除所有未关闭doc
                    // TODO: 结束文档
                }
            }
        }

    // test
//    GlobalEventChannel
//        .subscribeAlways<GroupMessageEvent> { groupMessageEvent ->
//            println("aa" + message.contentToString())
//            groupMessageEvent.message.filterIsInstance<MessageContent>()
//                .forEach {
////                    subject.sendMessage(it)
//                    if (it is FileMessage) {
//                        if (it.name.split(".").last() in listOf("jpg", "png", "jpeg")) {
//                            it.toAbsoluteFile(group)?.apply {
//                                val downloadUrl = getUrl()?.download(it.name)
//                                subject.sendMessage("下载完成")
//                            }
//                        }
//                    }
//                }
//        }
}

// message 解析


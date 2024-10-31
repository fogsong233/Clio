package tech.fogsong.docx

import kotlinx.coroutines.Dispatchers
import org.apache.poi.xwpf.usermodel.*
import tech.fogsong.storage.StorageConfig
import java.time.LocalTime
import javax.imageio.ImageIO

private const val A4_PAGE_WIDTH = 8420

class DocFlow private constructor() {

    private var overview: String? = null
    private var discussionMsgList = mutableListOf<DocMsgItem>()
    private var questionMsgList = mutableListOf<DocMsgItem>()
    private var summaryText: String? = null
    private var timeStr: String = LocalTime.now().toString()

    fun addQuestionMsg(docMsgItem: DocMsgItem): DocFlow {
        questionMsgList.add(docMsgItem)
        return this
    }

    fun addMultipleQuestionMsg(docMsgItems: List<DocMsgItem>): DocFlow {
        questionMsgList.addAll(docMsgItems)
        return this
    }

    fun addDiscussionMsg(docMsgItem: DocMsgItem): DocFlow {
        discussionMsgList.add(docMsgItem)
        return this
    }

    fun addMultipleDiscussionMsg(docMsgItems: List<DocMsgItem>): DocFlow {
        discussionMsgList.addAll(docMsgItems)
        return this
    }

    fun summaryText(summaryText: String): DocFlow {
        this.summaryText = summaryText
        return this
    }

    fun overview(overview: String): DocFlow {
        this.overview = overview
        return this
    }

    suspend fun build(groupId: Long): XWPFDocument {
        val xwpfDocument = XWPFDocument()
        // title
        val titleRun = xwpfDocument.createParagraph().run {
            alignment = ParagraphAlignment.CENTER
            createRun()
        }
        titleRun.apply {
            setText(StorageConfig.genDocName(overview, groupId))
            setFontSize(20)
            isBold = true
        }

        val genChatList = { msgList: List<DocMsgItem> ->
            msgList.forEach {
                val infoPara = xwpfDocument.createParagraph()
                // 消息的信息源
                infoPara.apply {
                    val userName = createStandardFontSizeRun()
                    userName.setYellowBgText(it.qqName)
                    val userId = createStandardFontSizeRun()
                    userId.setText(it.qqAccountID + "\n")
                    // 构建回复
                    it.callMsg?.let { callMsg ->
                        createStandardFontSizeRun().apply {
                            setText("回复")
                        }
                        createStandardFontSizeRun().apply {
                            setYellowBgText(callMsg.qqName)
                        }
                        createStandardFontSizeRun().apply {
                            val content = callMsg.contentList.first()
                            when (content.type) {
                                ChatMsgContent.MsgContentType.STRING -> {
                                    setGreyBgText("""${content.contentOrPath.slice(0..15)}...""")
                                }

                                ChatMsgContent.MsgContentType.IMAGE -> {
                                    setGreyBgText("[图片]")
                                }
                            }
                        }

                    }
                }
                // 正文
                it.contentList.forEach { content ->
                    val para = xwpfDocument.createParagraph()
                    when (content.type) {
                        ChatMsgContent.MsgContentType.STRING -> {
                            para.createStandardFontSizeRun().apply {
                                setText(content.contentOrPath)
                            }
                        }

                        ChatMsgContent.MsgContentType.IMAGE -> {
                            // 插入图片
                            val nameStr = content.contentOrPath
                            with(Dispatchers.IO) {
                                val imgPath = StorageConfig.getCacheImagePath(nameStr)
                                val originImage = ImageIO.read(imgPath)
                                val width = originImage.width
                                val height = originImage.height
                                imgPath.inputStream().use { inputStream ->
                                    para.createRun().apply {
                                        val finalWidth = (A4_PAGE_WIDTH * 0.55).toInt()
                                        val finalHeight = finalWidth / width * height
                                        addPicture(
                                            inputStream,
                                            Document.PICTURE_TYPE_JPEG,
                                            nameStr,
                                            finalWidth,
                                            finalHeight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                xwpfDocument.createParagraph().createStandardFontSizeRun().apply {
                    setText("---------------------------------------------------------------")
                }
            }
        }

        // 生成提问
        xwpfDocument.genSectionTitle("提问")
        genChatList(questionMsgList)
        xwpfDocument.genSectionTitle("讨论")
        genChatList(discussionMsgList)
        // 文字总结
        xwpfDocument.genSectionTitle("总结")
        xwpfDocument.createParagraph().createRun().apply {
            setText(summaryText)
            fontSize = 14
            color = "FF0000"
        }
        return xwpfDocument
    }

    companion object {
        fun create(): DocFlow {
            return DocFlow()
        }
    }
}

fun XWPFRun.setColorBgText(text: String, color: String) {
    setText(text)
    ctr.addNewRPr().addNewShd()?.apply {
        fill = color
    }
}

fun XWPFRun.setYellowBgText(text: String) = setColorBgText(text, "FFFF00")
fun XWPFRun.setGreyBgText(text: String) = setColorBgText(text, "D3D3D3")
fun XWPFRun.setRedBgText(text: String) = setColorBgText(text, "FF0000")

fun XWPFDocument.genSectionTitle(text: String) {
    createParagraph().createRun().apply {
        setRedBgText(text)
        fontSize = 14
    }
}

fun XWPFParagraph.createStandardFontSizeRun(): XWPFRun {
    return createRun().apply {
        fontSize = 11
    }
}

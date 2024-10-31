package tech.fogsong.docx

data class DocMsgItem(
    val qqName: String,
    val qqAccountID: String,
    val callMsg: DocMsgItem?,
    val contentList: List<ChatMsgContent>
)

data class ChatMsgContent(
    val type: MsgContentType,
    val contentOrPath: String
) {
    enum class MsgContentType {
        STRING, IMAGE
    }
}
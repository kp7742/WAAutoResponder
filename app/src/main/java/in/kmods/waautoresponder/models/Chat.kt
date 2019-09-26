package `in`.kmods.waautoresponder.models

data class Chat(
    //Common
    var message: String? = null,
    var media: Boolean = false,
    var media_caption: String? = null,
    var media_mime_type: String? = null,
    //Contact
    var jid: String? = null,
    var sender: String? = null,
    var number: String? = null,
    var status: String? = null,
    //Group
    var gid: String? = null,
    var group: Boolean = false,
    var group_name: String? = null,
    var group_desc: String? = null,
    var group_desc_setter: String? = null,
    //Quote
    var quote: Boolean = false,
    var quoted_jid: String? = null,
    var quoted_message: String? = null,
    //Mentioned
    var mentioned: Boolean = false,
    var mentioned_jids: Array<String>? = null
)
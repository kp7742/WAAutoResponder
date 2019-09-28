package `in`.kmods.waautoresponder.receivers

import `in`.kmods.waautoresponder.KMODs
import `in`.kmods.waautoresponder.KMODs.Companion.msgstore
import `in`.kmods.waautoresponder.KMODs.Companion.wacontact
import `in`.kmods.waautoresponder.Replies
import `in`.kmods.waautoresponder.activities.PrefKey
import `in`.kmods.waautoresponder.models.Chat
import `in`.kmods.waautoresponder.utilities.Utils
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.appizona.yehiahd.fastsave.FastSave
import java.lang.Exception

class NotificationReceiver : NotificationListenerService() {
    private lateinit var replies: Replies

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if(!Settings.Secure.getString(contentResolver,"enabled_notification_listeners").contains(packageName)
            || sbn == null || sbn.isOngoing || sbn.packageName != KMODs.WAPkg
            || !FastSave.getInstance().getBoolean(PrefKey.BOT_ENABLE, false)
        ) {return}

        val notification = sbn.notification ?: return

        val bundle = NotificationCompat.getExtras(notification) ?: return

        //Should be Chat Message Not Notification Group
        if(!bundle.containsKey("android.isGroupConversation")) {return}

        try {
            val chat = Chat()

            var query = "select key_remote_jid, message_table_id from chat_list order by sort_timestamp desc limit 1"
            val chatList = msgstore?.Query(query) ?: throw Exception("Null Chatlist")
            if(chatList.isEmpty){throw Exception("Empty Chatlist")}

            var remotejid = chatList.getString(0) ?: throw Exception("Empty RemoteJid")
            val msgId = chatList.getString(1) ?: throw Exception("Empty MsgId")

            if(remotejid.contains("@g.us")){
                chat.group = true
                chat.gid = remotejid
            } else {
                chat.jid = remotejid
                chat.number = Utils.jidToNum(remotejid)
            }

            //Check for Quoted and Mentioned Message Data
            query = "select data, remote_resource, media_size, media_caption, media_mime_type, mentioned_jids, quoted_row_id from messages where _id = '$msgId'"
            val messages = msgstore?.Query(query) ?: throw Exception("Null Message Data - $msgId")
            if(messages.isEmpty){throw Exception("Empty Message Data - $msgId")}

            chat.message = messages.getString(0)

            if(chat.group){
                remotejid = messages.getString(1) ?: throw Exception("Empty Remote Resource")
                chat.jid = remotejid
                chat.number = Utils.jidToNum(remotejid)
            }

            messages.getString(2)?.let {
                if(it.toInt() > 0){
                    chat.media = true
                    chat.media_caption = messages.getString(3)
                    chat.media_mime_type = messages.getString(4)
                }
            }

            //Lookup in Whatsapp Contacts for Sender Data
            query = "select display_name, wa_name, status from wa_contacts where jid = '${chat.jid}'"
            val waContacts = wacontact?.Query(query) ?: throw Exception("Null WA Contacts - ${chat.jid}")
            if(waContacts.isEmpty){throw Exception("Empty WA Contacts - ${chat.jid}")}

            val displayName = waContacts.getString(0)
            val waName = waContacts.getString(1)

            chat.status = waContacts.getString(2)

            chat.sender = if (waName != null && waName.isNotEmpty()) {
                waName
            } else if (displayName != null && displayName.isNotEmpty()) {
                displayName
            } else {
                chat.number
            }

            if(chat.group) {
                //Lookup in Whatsapp Contacts for Sender Data
                query = "select display_name from wa_contacts where jid = '${chat.gid}'"
                val waGroups = wacontact?.Query(query) ?: throw Exception("Null WA Groups - ${chat.gid}")
                if(waGroups.isEmpty){throw Exception("Empty WA Groups - ${chat.gid}")}

                chat.group_name = waGroups.getString(0)

                //Lookup for Whatsapp Group Description
                query = "select description, description_setter_jid from wa_group_descriptions where jid = '${chat.gid}'"
                val waGroupDesc = wacontact?.Query(query) ?: throw Exception("Null WA Group Desc - ${chat.gid}")
                if (!waGroupDesc.isEmpty) {
                    chat.group_desc = waGroupDesc.getString(0)
                    chat.group_desc_setter = waGroupDesc.getString(1)
                }
            }

            //Process Mentioned Message Data
            messages.getString(5)?.let {
                if(it.isNotEmpty()){
                    chat.mentioned = true
                    chat.mentioned_jids = it.split(",").toTypedArray()
                }
            }

            //Process Quoted Message Data
            messages.getString(6)?.let {
                if(it.toInt() > 0){
                    query = "select data, remote_resource from messages_quotes where _id = '$it'"
                    val messagesQuotes = msgstore?.Query(query) ?: throw Exception("Null Message Quotes - $it")
                    if(messagesQuotes.isEmpty){throw Exception("Empty Message Quotes - $it")}

                    messagesQuotes.getString(0)?.let { quote ->
                        if (quote.isNotEmpty()) {
                            chat.quote = true
                            chat.quoted_message = quote
                            chat.quoted_jid = messagesQuotes.getString(1)
                        }
                    }
                }
            }

            replies.reply(notification, chat)
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, "WAAutoResponder Failed To Read!: $e", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate() {
        super.onCreate()
        replies = Replies(this)
    }
}
package `in`.kmods.waautoresponder

import `in`.kmods.waautoresponder.activities.PrefKey
import `in`.kmods.waautoresponder.models.Chat
import `in`.kmods.waautoresponder.utilities.Utils
import android.app.Notification
import android.content.ContentValues
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.appizona.yehiahd.fastsave.FastSave
import java.lang.Exception

class Replies(ctx: Context) {
    var contacts: ContentValues
    var context: Context
    lateinit var notification : Notification
    lateinit var chat : Chat

    init {
        context = ctx
        contacts = Utils.contactFetch(ctx)
    }

    fun reply(noti: Notification, chatdata: Chat){
        notification = noti
        chat = chatdata

        if(FastSave.getInstance().getBoolean(PrefKey.TOAST_ENABLE, false)) {
            val toast =
                (if (chat.group) chat.group_name + " | " + chat.sender else chat.sender) + " | " +
                        if (chat.media) {
                            if (chat.media_mime_type!!.contains("image")) {
                                "Image" + if (chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                            } else if (chat.media_mime_type!!.contains("video")) {
                                "Video" + if (chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                            } else if (chat.media_mime_type!!.contains("audio")) {
                                "Audio" + if (chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                            } else if (chat.media_mime_type!!.contains("application")) {
                                "File" + if (chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                            } else {
                                chat.media_caption
                            }
                        } else {
                            if (chat.message!!.isEmpty() && !chat.media && !chat.quote) "Deleted!" else chat.message
                        }
            Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
        }

        if(chat.sender != null && chat.message != null) {
            val sender = chat.sender
            val message = chat.message

            CommonReply(message)
        }
    }

    fun CommonReply(text: String?){
        when(text) {
            "@helpkp" -> {
                sendMsg("*KP's Bot Commands* :-\n\n" +
                        "@emailkp - to get my official EMail Address.\n\n" +
                        "@webkp - to get my offical website link.\n\n" +
                        "@pinkp - to get my pinned message.")
            }
            "@emailkp" -> {
                sendMsg("*KP's Official EMail* :- \n\npatel.kuldip91@gmail.com")
            }
            "@webkp" -> {
                sendMsg("*KP's Official Website* :- \n\nhttps://kuldippatel.dev/")
            }
            "@pinkp" -> {
                sendMsg(FastSave.getInstance().getString(PrefKey.COMMON_PINNED_MSG, ""))
            }
        }
    }

    //Send Reply Message Through Notification
    fun sendMsg(msg: String) {
        val wearableExtender = NotificationCompat.WearableExtender(notification)
        val nactions: Array<NotificationCompat.Action> = wearableExtender.actions.toTypedArray()

        var notiAction: NotificationCompat.Action? = null
        val remoteInput = mutableListOf<RemoteInput>()

        nactions.forEach { nact ->
            val chatname = nact.title.toString().replace("Reply to ","")
            if((chat.group && chatname.contains(chat.group_name as CharSequence))
                || (!chat.group && (chatname.contains(chat.sender as CharSequence)
                        || chatname.replace("[ ()-]", "").contains(chat.number as CharSequence)
                        || contacts.containsKey(chatname)))) {
                Log.e("WABOT", "NAction Name: ${nact.title}")
                notiAction = nact
                nact.remoteInputs?.forEach { remote ->
                    if (remote.allowFreeFormInput) {
                        remoteInput.add(remote)
                    }
                }
            }
        }

        if(notiAction != null) {
            val localIntent = Intent()
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            remoteInput.forEach {
                notification.extras.putCharSequence(it.resultKey, msg)
            }

            RemoteInput.addResultsToIntent(
                remoteInput.toTypedArray(),
                localIntent,
                notification.extras
            )
            notiAction!!.actionIntent.send(context, 0, localIntent)
        }
    }

    //Get Contact Details from JID
    fun getContactDetails(jid: String): Array<String> {
        val query = "select display_name, wa_name, status from wa_contacts where jid = '$jid'"
        var name = ""
        var status = ""
        KMODs.wacontact?.Query(query)!!.let {data ->
            if(!data.isEmpty){
                val displayName = data.getString(0)
                val waName = data.getString(1)

                name = if (waName != null && waName.isNotEmpty()) {
                    waName
                } else if (displayName != null && displayName.isNotEmpty()) {
                    displayName
                } else ""
                status = data.getString(2)!!
            }
        }
        return arrayOf(name, status)
    }

    //Finding Group Admins
    fun getGroupAdmins(gid: String): Array<String> {
        val adminList = mutableListOf<String>()
        val query = "select jid, admin from group_participants where gjid = '$gid' and (admin = 1 or admin = 2);"
        KMODs.msgstore?.Query(query)!!.let {groupsAdmins ->
            if(!groupsAdmins.isEmpty){
                do {
                    val admin = groupsAdmins.getString(0) ?: continue
                    adminList.add(if(admin.isNotEmpty()) admin else "919998897742@s.whatsapp.net")
                } while (groupsAdmins.nextRow())
            }
        }
        return adminList.toTypedArray()
    }
}
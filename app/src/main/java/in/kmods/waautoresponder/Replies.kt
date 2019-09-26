package `in`.kmods.waautoresponder

import `in`.kmods.waautoresponder.models.Chat
import `in`.kmods.waautoresponder.utilities.Utils
import android.app.Notification
import android.content.ContentValues
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.lang.Exception

class Replies(ctx: Context) {
    var contacts: ContentValues
    var context: Context

    init {
        context = ctx
        contacts = Utils.contactFetch(ctx)
    }

    fun reply(noti: Notification, chat: Chat){
        val toast = (if(chat.group) chat.group_name + " | " + chat.sender else chat.sender) + " | " +
                (if(chat.media) {
                    if(chat.media_mime_type!!.contains("image")) {
                        "Image" + if(chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                    } else if(chat.media_mime_type!!.contains("video")) {
                        "Video" + if(chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                    } else if(chat.media_mime_type!!.contains("audio")) {
                        "Audio" + if(chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                    } else if(chat.media_mime_type!!.contains("application")) {
                        "File" + if(chat.media_caption!!.isNotEmpty()) ": ${chat.media_caption}" else ""
                    } else {
                        chat.media_caption
                    }
                } else {
                    if(chat.message!!.isEmpty() && !chat.media && !chat.quote) "Deleted!" else chat.message
                })
        Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
        /*
        val nactions: Array<NotificationCompat.Action> = Array(NotificationCompat.getActionCount(noti)) { i: Int ->
            NotificationCompat.getAction(noti, i)
        }

        var notiAction: NotificationCompat.Action? = null
        var remoteInput: RemoteInput? = null


        nactions.forEach {
            if(it.title.contains("Reply")){
                notiAction = it
                if(it.remoteInputs != null){
                    it.remoteInputs.forEach {
                        if(it.label.contains("Reply") && it.allowFreeFormInput){
                            remoteInput = it
                        }
                    }
                }
            }
        }

        if(notiAction != null && remoteInput != null){
            val localIntent = Intent()
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            noti.extras.putCharSequence(remoteInput!!.resultKey, "Hello")

            RemoteInput.addResultsToIntent(arrayOf(remoteInput), localIntent, noti.extras)
            notiAction!!.actionIntent.send(context, 0, localIntent)
        }*/
    }

    //Finding Group Admins
    fun getGroupAdmins(chat: Chat): Array<String> {
        val adminList = mutableListOf<String>()
        val query = "select jid, admin from group_participants where gjid = '${chat.gid}' and (admin = 1 or admin = 2);"
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
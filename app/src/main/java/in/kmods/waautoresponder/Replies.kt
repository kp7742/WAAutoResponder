package `in`.kmods.waautoresponder

import `in`.kmods.waautoresponder.activities.PrefKey
import `in`.kmods.waautoresponder.models.Chat
import `in`.kmods.waautoresponder.utilities.Utils
import `in`.kmods.waautoresponder.utilities.evalex.Expression
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

        if(Utils.getBoolPref(PrefKey.TOAST_ENABLE)) {
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

        val sender = chat.sender!!
        val message = chat.message!!

        CommonReply(message)
    }

    fun CommonReply(text: String){
        when(text) {
            "@helpkp" -> {
                sendMsg("*KP's Bot Commands* :-\n\n" +
                        "@emailkp - to get my official EMail Address.\n\n" +
                        "@webkp - to get my offical website link.\n\n" +
                        "@pinkp - to get my pinned message.\n\n" +
                        "@calc {Expression} - to do calculations like +,-,*,/,% ,\n\n " +
                        "sin(),cos(),tan(),cot(),sec(),csc()[cosec] ,\n\n " +
                        "deg()[Radian to Degree],rad()[Degree to Radian],fact()[Factorial] ,\n\n " +
                        "sqrt()[SquareRoot], log()[log base e], log10()[log base 10] ,\n\n " +
                        "MIN(e1,e2, ...)[for mininmum of given values], MAX(e1,e2, ...)[for maximum of given values] ,\n\n " +
                        "Eg. @calc 3+2\n\n" +
                        "@mystatus - to get own status.(Wait for System to Fetch, It may Fail)\n\n" +
                        "@mydata - to get basic data of your current info.(Wait for System to Fetch, It may Fail)\n\n" +
                        "@data - to get basic data of any mentioned or quoted person.(Wait for System to Fetch, It may Fail)\n\n" +
                        "@admins - to get list of admins of group(Only for Group).\n\n" +
                        "@desc - to get Description of Group(Only for Group).")
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
            "@mystatus" -> {
                if(chat.status != null && chat.status!!.isNotEmpty()){
                    sendMsg("*KP's Bot* :-\n\nYour Status is '" + chat.status + "'")
                } else {
                    sendMsg("*KP's Bot* :-\n\nSorry, I did't get Your Status")
                }
            }
            "@mydata" -> {
                if(chat.jid != null && chat.jid!!.isNotEmpty()){
                    sendMsg("*KP's Bot* :-\n\nYour Name is ${chat.sender}" +
                            (if (chat.status!!.isNotEmpty()) "\n\nYour Status is '${chat.status}'" else "\n\nSorry, I did't get Your Status") +
                            "\n\nYour Number is ${chat.number}" +
                            (if (chat.group) "\n\nThis Group Name is ${chat.group_name}" else ""))
                } else {
                    sendMsg("*KP's Bot* :-\n\nSorry, I don't get Your Data")
                }
            }
            "@desc" -> {
                if(chat.group) {
                    if (chat.group_desc != null && chat.group_desc!!.isNotEmpty()) {
                        sendMsg("${chat.group_desc}\n\nSet By ${Utils.jidToNum(chat.group_desc_setter!!)}")
                    } else {
                        sendMsg("*This Group Has No Description*")
                    }
                }
            }
            "@admins" -> {
                if(chat.group) {
                    val adminList = getGroupAdmins(chat.gid!!)
                    var msg = "*Fail to get Admin Data*"
                    if(adminList.isNotEmpty()){
                        val stringBuilder = StringBuilder("*${chat.group_name} Group Admins* :-\n\n")
                        for(i in 0 until adminList.size) {
                            val jid = adminList.get(i)
                            val name = getContactDetails(jid)[0]
                            stringBuilder.append((i+1)).append(". ").append(Utils.jidToNum(jid)).append(" - ").append(name).append("\n")
                        }
                        msg = stringBuilder.toString()
                    }
                    sendMsg(msg)
                }
            }
        }
        if(Utils.getBoolPref(PrefKey.CALC_ENABLE) && text.startsWith("@calc")){
            val problm = text.replace("@calc ","")
            try {
                val result = Expression(problm).eval().toString()
                sendMsg("Answer of $problm is $result")
            } catch (e: Exception){
                sendMsg("$problm is Invalid Expression")
            }
        }
        if(text.startsWith("@data")){
            val jid = if(chat.quote && chat.quoted_jid != null) chat.quoted_jid
            else if(chat.mentioned) chat.mentioned_jids!![0] else null

            if(jid != null && jid.isNotEmpty()){
                val cdata = getContactDetails(jid)
                sendMsg("*KP's Bot* :-\n\nHello ${chat.sender}, Your Requested Data:-\n\n"  +
                        "Name = ${cdata[0]}" +
                        (if (cdata[1].isNotEmpty()) "\n\nStatus = '${cdata[1]}'" else "\n\nSorry, I did't get Your Status") +
                        "\n\nNumber = ${Utils.jidToNum(jid)}" +
                        (if (chat.group) "\n\nThis Group Name = ${chat.group_name}" else ""))
            } else {
                sendMsg("*KP's Bot* :-\n\nSorry, I don't get Data")
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
package `in`.kmods.waautoresponder.utilities

import android.annotation.SuppressLint
import android.provider.ContactsContract
import android.content.ContentValues
import android.content.Context
import com.appizona.yehiahd.fastsave.FastSave

class Utils {
    companion object {
        fun jidToNum(jid: String): String {
            return "+" + jid.replace("@s.whatsapp.net", "")
        }

        fun getBoolPref(key: String): Boolean {
            return FastSave.getInstance().getBoolean(key, false)
        }

        @SuppressLint("Recycle")
        fun contactFetch(ctx: Context): ContentValues {
            val contacts = ContentValues()
            val c = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                ContactsContract.RawContacts.ACCOUNT_TYPE + " <> 'google' ",
                null,
                null
            ) ?: return contacts
            if (c.getCount() < 1) {return contacts}
            while (c.moveToNext()) {
                val Phone_number = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                contacts.put(name, Phone_number)
            }
            c.close()
            return contacts
        }
    }
}
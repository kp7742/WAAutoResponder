package `in`.kmods.waautoresponder

import `in`.kmods.waautoresponder.databases.RootSQLiteDB
import android.app.Application
import com.appizona.yehiahd.fastsave.FastSave
import com.topjohnwu.superuser.Shell

class KMODs : Application() {
    companion object {
        val WAPkg = "com.whatsapp"
        var msgstore: RootSQLiteDB? = null
        var wacontact: RootSQLiteDB? = null
    }

    override fun onCreate() {
        super.onCreate()
        FastSave.init(getApplicationContext())
        msgstore = RootSQLiteDB.getInstance(this, WAPkg,"msgstore.db")
        wacontact = RootSQLiteDB.getInstance(this, WAPkg,"wa.db")
    }
}
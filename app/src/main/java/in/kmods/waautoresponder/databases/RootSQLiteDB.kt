package `in`.kmods.waautoresponder.databases

import android.content.Context
import com.topjohnwu.superuser.Shell
import java.io.File
import kotlin.collections.ArrayList

class RootSQLiteDB(ctx: Context, pkg: String, db: String) {
    private var libPath = ""
    private var dbName = db
    private var dbPath = ""

    init {
        libPath = ctx.applicationInfo.nativeLibraryDir + "/libsqlite3.so"
        dbPath = ctx.applicationInfo.dataDir.replace(ctx.packageName, pkg) + "/databases/" + dbName
    }

    companion object {
        /*
        * Check for Database and Return Instance for Access
        */
        fun getInstance(ctx: Context, pkgname: String, DB: String): RootSQLiteDB? {
            if (Shell.rootAccess()) {
                Shell.su("setenforce 0").submit()
                if (File(ctx.applicationInfo.dataDir.replace(ctx.packageName, pkgname) + "/databases/" + DB).exists()) {
                    return RootSQLiteDB(ctx, pkgname, DB)
                }
            }
            return null
        }
    }

    /*
    * Fire Select Query, Results Stores in Cursor
    */
    fun Query(query: String): Cursor {
        val result = Shell.su("$libPath $dbPath \"$query\"").exec()
        return Cursor(result.out)
    }

    /*
     * Fire Query Without Any Result
     */
    fun ExecQuery(query: String): Boolean {
        return Shell.su("$libPath $dbPath \"$query\"").exec().isSuccess
    }

    /*
     * Database Name
     */
    fun getDBName(): String {
        return dbName
    }

    /*
     * Custom Cursor Implementation Similar to SQlite Cursor Java Api
     */
    inner class Cursor (data: List<String>) {//Reads CSV Like Data
        private val tableviewdata: List<List<String>>//Virtual Implementation of TableView
        private var ptr = 0//Current Row Cursor Pointer

        /*
         * Check If TableView is Empty or Not
         */
        val isEmpty: Boolean
            get() = tableviewdata.isEmpty()

        init {
            tableviewdata = ArrayList()
            for (d in data) {
                tableviewdata.add(ArrayList(d.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toList()))
            }
        }

        /*
         * Get String from Current Row By Column
         * Column Index is Start from 0.
         * Invalid Coumn Index Results Null
         */
        fun getString(column: Int): String? {
            if (ptr < 0 && ptr > count()) {
                return null
            }
            val row = tableviewdata[ptr]
            return if (column >= row.size) {
                null
            } else row[column]
        }

        /*
         * Move Current Cursor to Next Row
         * Cursor out of range returns false
         */
        fun nextRow(): Boolean {
            if (ptr + 1 < count()) {
                ptr++
                return true
            }
            return false
        }

        /*
         * Move Current Cursor to Previous Row
         * Cursor out of range returns false
         */
        fun prevRow(): Boolean {
            if (ptr - 1 >= 0) {
                ptr--
                return true
            }
            return false
        }

        /*
         * Give Total Number of Rows in TableView
         */
        fun count(): Int {
            return tableviewdata.size
        }
    }
}
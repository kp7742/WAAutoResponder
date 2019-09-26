package `in`.kmods.waautoresponder.activities

import `in`.kmods.waautoresponder.BuildConfig
import `in`.kmods.waautoresponder.KMODs
import `in`.kmods.waautoresponder.R
import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.anggrayudi.materialpreference.PreferenceActivityMaterial
import com.anggrayudi.materialpreference.PreferenceFragmentMaterial
import com.anggrayudi.materialpreference.annotation.PreferenceKeysConfig
import com.topjohnwu.superuser.Shell
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.appizona.yehiahd.fastsave.FastSave
import net.codecision.startask.permissions.Permission

class KMODsActivity : PreferenceActivityMaterial() {
    private val TAG = "WABOT"
    private var settingsFragment: SettingsFragment? = null
    private val permission: Permission by lazy {
        Permission.Builder(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .setRequestCode(100)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shell.rootAccess()
        if(!Settings.Secure.getString(contentResolver,"enabled_notification_listeners").contains(packageName)){
            Toast.makeText(this, "Please Enable Notification Access", Toast.LENGTH_LONG).show()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        permission.check(this)
            .onShowRationale {
                permission.request(this)
            }
        onCheck()
        if (savedInstanceState == null) {
            settingsFragment =
                SettingsFragment.newInstance(
                    null
                )
            supportFragmentManager.beginTransaction().add(android.R.id.content, settingsFragment!!, TAG).commit()
        } else {
            onBackStackChanged()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permission.onRequestPermissionsResult(this, requestCode, grantResults)
            .onDenied {
                AlertDialog.Builder(this)
                    .setTitle("Accept All Permission!")
                    .setMessage("Please make sure your allow all Permission for Perfect Working of this App.")
                    .setNeutralButton("Ok") { dialog, _ ->
                        dialog.dismiss()
                        permission.request(this)
                    }
                    .setCancelable(false)
                    .create()
                    .show()
                FastSave.getInstance().saveBoolean(PrefKey.BOT_ENABLE, false)
            }.onNeverAskAgain {
                AlertDialog.Builder(this)
                    .setTitle("Accept All Permission!")
                    .setMessage("Please make sure your allow all Permission for Perfect Working of this App.")
                    .setNeutralButton("Ok") { dialog, _ ->
                        dialog.dismiss()
                        permission.request(this)
                    }
                    .setCancelable(false)
                    .create()
                    .show()
                FastSave.getInstance().saveBoolean(PrefKey.BOT_ENABLE, false)
            }
    }

    override fun onBackStackChanged() {
        settingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
        title = settingsFragment!!.preferenceFragmentTitle
    }

    override fun onBuildPreferenceFragment(rootKey: String?): PreferenceFragmentMaterial {
        return SettingsFragment.newInstance(
            rootKey
        )
    }

    @PreferenceKeysConfig
    class SettingsFragment : PreferenceFragmentMaterial() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)
            findPreference(PrefKey.ABOUT)!!.title = getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME
        }

        companion object {
            fun newInstance(rootKey: String?): SettingsFragment {
                val args = Bundle()
                args.putString(ARG_PREFERENCE_ROOT, rootKey)
                val fragment =
                    SettingsFragment()
                fragment.arguments = args
                return fragment
            }
        }
    }

    private fun onCheck() {
        if(!FastSave.getInstance().isKeyExists(PrefKey.COMMON_PINNED_MSG)){
            FastSave.getInstance().saveString(PrefKey.COMMON_PINNED_MSG, "_No Pinned Msg Right Now_")
        }
        if (!Shell.rootAccess()) {
            AlertDialog.Builder(this)
                .setTitle("Root Not Found!!!")
                .setMessage("Please make sure your phone is rooted and you given root permission to this app.")
                .setNeutralButton("Ok") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .create()
                .show()
            FastSave.getInstance().saveBoolean(PrefKey.BOT_ENABLE, false)
        } else if(!isInstalled()){
            AlertDialog.Builder(this)
                .setTitle("App Not Found!!!")
                .setMessage("Please install the WhatsApp to continue with this app.")
                .setNeutralButton("Ok") { _, _ ->
                    startActivity(Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + KMODs.WAPkg)))
                    finish()
                }
                .setCancelable(false)
                .create()
                .show()
            FastSave.getInstance().saveBoolean(PrefKey.BOT_ENABLE, false)
        }
    }

    private fun isInstalled(): Boolean {
        return packageManager.getLaunchIntentForPackage(KMODs.WAPkg) != null
    }
}

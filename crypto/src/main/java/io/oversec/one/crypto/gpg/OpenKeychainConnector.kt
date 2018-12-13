package io.oversec.one.crypto.gpg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import io.oversec.one.crypto.AppsReceiver
import org.openintents.openpgp.IOpenPgpService2
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi
import roboguice.util.Ln
import java.io.InputStream
import java.io.OutputStream
import java.util.HashSet


class OpenKeychainConnector(val ctx: Context) {

    private var mApi: OpenPgpApi? = null
    private var mService: IOpenPgpService2? = null
    private var mServiceConnection: ServiceConnection? = null


    //    public static void onPackageStatusChanged(Context context, String action, String packagename) {
    //        if (action==Intent.ACTION_PACKAGE_ADDED || action==Intent.ACTION_PACKAGE_CHANGED) {
    //            if (PACKAGE_NAME.equals(packagename)) {
    //                //seems like Open-Keychain just got installed or re-enabled
    //                init(context);
    //            }
    //        }
    //    }

    init {
        AppsReceiver.addListener(object : AppsReceiver.IAppsReceiverListener {
            override fun onAppChanged(ctx: Context, action: String, packagename: String) {
                if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_CHANGED) {
                    if (PACKAGE_NAME == packagename) {
                        //seems like Open-Keychain just got installed or re-enabled
                        init()
                    }
                }
            }
        })
    }


    fun init() {
        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Ln.w("OKCS: Service Connected!")
                mService = IOpenPgpService2.Stub.asInterface(service)
                mApi = OpenPgpApi(ctx, mService)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Ln.w("OKCS: Service Disconnected!")
                mService = null
                mApi = null
            }
        }

        bindToService()
    }

    @Synchronized
    fun executeApi(data: Intent, inputStream: InputStream?, outputStream: OutputStream?): Intent {
        Ln.w("OKCS: executeAPI... %s", data.action)
        if (mApi == null) {

            val res = Intent()
            res.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)
            val error = OpenPgpError(0, "OKC Service Not Bound, try again!!")
            res.putExtra(OpenPgpApi.RESULT_ERROR, error)

            //try to restart
            bindToService()

            return res
        }
        else {
            val res = mApi!!.executeApi(data, inputStream, outputStream)
            Ln.w("OKCS: ...executeAPI")
            return res
        }
    }


    @Synchronized
    private fun bindToService() {
        Ln.d(Exception(), "DAMNIT OKCS: bindToService...")
        // if not already bound...
        if (mService == null) {


            if (getVersion() < V_MIN) {
                Ln.w(
                    "OKCS: not trying to connect to unsupported Open-Keychain Version %s",
                    getVersion()
                )
            } else {


                try {

                    val serviceIntent = Intent(OpenPgpApi.SERVICE_INTENT_2)
                    // NOTE: setPackage is very important to restrict the intent to this provider only!
                    serviceIntent.setPackage(PACKAGE_NAME)
                    val connect = ctx.bindService(
                        serviceIntent, mServiceConnection!!,
                        Context.BIND_AUTO_CREATE
                    )
                    if (!connect) {
                        Ln.w("OKCS: Service couldn't be connected to, connect=false!")
                    }
                } catch (e: Exception) {
                    Ln.e(e, "OKCS: Service couldn't be connected to")
                }

            }
        } else {
            Ln.w("OKCS: Service already/still bound")
        }
    }

    @Synchronized
    private fun unbindFromService() {
        if (mServiceConnection != null) {
            ctx.unbindService(mServiceConnection!!)
        }
    }

    fun isInstalled(): Boolean {
        return getVersion() >= 0
    }

    fun getVersion(): Int {
        val intent = Intent(OpenPgpApi.SERVICE_INTENT_2)
        intent.setPackage(PACKAGE_NAME)
        val resInfo = ctx.packageManager.queryIntentServices(intent, 0)
        var res = -1
        if (resInfo != null && !resInfo.isEmpty()) {
            val r = resInfo[0]
            val si = r.serviceInfo
            if (si != null) {
                val pn = si.packageName
                try {
                    res = ctx.packageManager
                        .getPackageInfo(pn, 0).versionCode
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

            }
        }
        return res
    }

    fun getVersionName(): String? {
        val intent = Intent(OpenPgpApi.SERVICE_INTENT_2)
        intent.setPackage(PACKAGE_NAME)
        val resInfo = ctx.packageManager.queryIntentServices(intent, 0)
        var res: String? = null
        if (!resInfo.isEmpty()) {
            val r = resInfo[0]
            val si = r.serviceInfo
            if (si != null) {
                val pn = si.packageName
                try {
                    res = ctx.packageManager
                        .getPackageInfo(pn, 0).versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }

            }
        }
        return res
    }

    fun allPackageNames(): Set<String> {
        val res = HashSet<String>()
        res.add(PACKAGE_NAME)
        res.add("org.sufficientlysecure.keychain.debug")
        return res
    }

    fun doPanicExit() {
        try {
            val panicButtonIntent = Intent("info.guardianproject.panic.action.TRIGGER")
            panicButtonIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            panicButtonIntent.setPackage(PACKAGE_NAME)
            ctx.startActivity(panicButtonIntent)
        } catch (ex: Exception) {
            //ignore, probably not installed
        }

    }

    fun isGooglePlayInstalled(): Boolean {
        val pm = ctx.packageManager
        try {
            pm.getPackageInfo("com.android.vending", 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }

    }

    fun openInPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE_NAME"))
            ctx.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    fun openInFdroid() {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("http://www.f-droid.org/app/$PACKAGE_NAME"))
            ctx.startActivity(intent)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    companion object {
        const val PACKAGE_NAME =
            "org.sufficientlysecure.keychain" //"org.sufficientlysecure.keychain.debug";

        const val V_MIN = 39500  //The minimum version we support, kind of arbitrary decision
        const val V_GET_SUBKEY = 39600

        @SuppressLint("StaticFieldLeak") // note that we're storing *Application*context
        @Volatile
        private var INSTANCE: OpenKeychainConnector? = null

        fun getInstance(ctx: Context): OpenKeychainConnector =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: OpenKeychainConnector(ctx.applicationContext).also { INSTANCE = it }.also { it.init() }
            }

    }

}

package io.oversec.one.crypto

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import com.nulabinc.zxcvbn.Zxcvbn
import roboguice.util.Ln


/**
 * Crazy workaround due to Zxcvbn keeping megabytes of dictionary data on the heap and has no way to free it -
 * so we're wrapping it with a service in a separate process
 */
class ZxcvbnService : Service() {

    private var mZxcvbn: Zxcvbn? = null

    private val mBinder = object : IZxcvbnService.Stub() {

        @Throws(RemoteException::class)
        override fun calcEntropy(aString: String): ZxcvbnResult {
            val strength = mZxcvbn!!.measure(aString)
            val feedback = strength.feedback
            val entropy = log2(strength.guesses).toInt()
            return ZxcvbnResult(feedback.warning, entropy)
        }

        @Throws(RemoteException::class)
        override fun exit() {
            Ln.d("exiting")
            stopSelf()

        }
    }

    override fun onCreate() {
        Ln.d("create")
        super.onCreate()
        mZxcvbn = Zxcvbn()
    }

    override fun onDestroy() {
        Ln.d("destroy")
        super.onDestroy()
        System.exit(0)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    companion object {

        fun log2(n: Double): Double {
            return Math.log(n) / Math.log(2.0)
        }
    }
}

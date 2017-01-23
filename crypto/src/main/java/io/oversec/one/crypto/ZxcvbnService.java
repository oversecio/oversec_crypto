package io.oversec.one.crypto;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import roboguice.util.Ln;


/**
 * Crazy workaround due to Zxcvbn keeping megabytes of dictionary data on the heap and has no way to free it -
 * so we're wrapping it with a service in a separate process
 *
 *
 * Created by yao on 17/01/17.
 */

public class ZxcvbnService extends Service {

    private Zxcvbn mZxcvbn;

    @Override
    public void onCreate() {
        Ln.d("create");
        super.onCreate();
        mZxcvbn = new Zxcvbn();
    }

    @Override
    public void onDestroy() {
        Ln.d("destroy");
        super.onDestroy();
        System.exit(0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       // Ln.d("Received start command.");
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
       // Ln.d("Received binding.");
        return mBinder;
    }

    private final IZxcvbnService.Stub mBinder = new IZxcvbnService.Stub() {

        @Override
        public ZxcvbnResult calcEntropy(String aString) throws RemoteException {
           // Ln.d("calcEntropy");
            Strength strength = mZxcvbn.measure(aString);
            Feedback feedback = strength.getFeedback();
            int entropy = (int) log2(strength.getGuesses());
            return new ZxcvbnResult(feedback.getWarning(),entropy);
        }

        @Override
        public void exit() throws RemoteException {
            Ln.d("exiting");
            stopSelf();

        }
    };

    public static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }
}

package io.oversec.one.crypto.gpg;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.IBinder;
import io.oversec.one.crypto.AppsReceiver;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import roboguice.util.Ln;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenKeychainConnector {
    public static final String PACKAGE_NAME = "org.sufficientlysecure.keychain";
    //public static final String PACKAGE_NAME = "org.sufficientlysecure.keychain.debug";

    public final static int V_MIN = 39500;  //The minimum version we support, kind of arbitrary decision
    public final static int V_GET_SUBKEY = 39600;


    private static OpenPgpApi mApi;
    private static IOpenPgpService2 mService;
    private static ServiceConnection mServiceConnection;
    private static Context mCtx;

//    public static void onPackageStatusChanged(Context context, String action, String packagename) {
//        if (action==Intent.ACTION_PACKAGE_ADDED || action==Intent.ACTION_PACKAGE_CHANGED) {
//            if (PACKAGE_NAME.equals(packagename)) {
//                //seems like Open-Keychain just got installed or re-enabled
//                init(context);
//            }
//        }
//    }

    static {
        AppsReceiver.addListener(new AppsReceiver.IAppsReceiverListener() {
            @Override
            public void onAppChanged(Context ctx, String action, String packagename) {
                if (action.equals(Intent.ACTION_PACKAGE_ADDED) || action.equals(Intent.ACTION_PACKAGE_CHANGED)) {
                    if (PACKAGE_NAME.equals(packagename)) {
                        //seems like Open-Keychain just got installed or re-enabled
                        init(ctx);
                    }
                }
            }
        });
    }

    public static synchronized void init(final Context ctx) {
        if (mCtx == null) {
            mCtx = ctx.getApplicationContext();

            mServiceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Ln.w("OKCS: Service Connected!");
                    mService = IOpenPgpService2.Stub.asInterface(service);
                    mApi = new OpenPgpApi(mCtx, mService);
                }

                public void onServiceDisconnected(ComponentName name) {
                    Ln.w("OKCS: Service Disconnected!");
                    mService = null;
                    mApi = null;
                }
            };
        }


        bindToService(mCtx);
    }

    public static synchronized Intent executeApi(Intent data, InputStream is, OutputStream os) {
        Ln.w("OKCS: executeAPI... %s", data.getAction());
        if (mApi == null) {

            Intent res = new Intent();
            res.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            OpenPgpError error = new OpenPgpError(0, "OKC Service Not Bound, try again!!");
            res.putExtra(OpenPgpApi.RESULT_ERROR, error);

            //try to restart
            if (mCtx != null) {
                bindToService(mCtx);
            }

            return res;
        }

        Intent res = mApi.executeApi(data, is, os);
        Ln.w("OKCS: ...executeAPI");
        return res;
    }


    private static synchronized void bindToService(Context ctx) {
        Ln.w("OKCS: bindToService...");
        // if not already bound...
        if (mService == null) {


            if (getVersion(ctx) < V_MIN) {
                Ln.w("OKCS: not trying to connect to unsupported Open-Keychain Version %s", getVersion(ctx));
            } else {


                try {

                    Intent serviceIntent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
                    // NOTE: setPackage is very important to restrict the intent to this provider only!
                    serviceIntent.setPackage(PACKAGE_NAME);
                    boolean connect = ctx.bindService(serviceIntent, mServiceConnection,
                            Context.BIND_AUTO_CREATE);
                    if (!connect) {
                        Ln.w("OKCS: Service couldn't be connected to, connect=false!");
                    }
                } catch (Exception e) {
                    Ln.e(e, "OKCS: Service couldn't be connected to");
                }
            }
        } else {
            Ln.w("OKCS: Service already/still bound");
        }
    }

    private static synchronized void unbindFromService() {
        if (mCtx != null && mServiceConnection != null) {
            mCtx.unbindService(mServiceConnection);
        }
    }

    public static boolean isInstalled(Context ctx) {
        return getVersion(ctx) >= 0;
    }

    public static int getVersion(Context context) {
        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
        intent.setPackage(PACKAGE_NAME);
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentServices(intent, 0);
        int res = -1;
        if (resInfo != null && !resInfo.isEmpty()) {
            ResolveInfo r = resInfo.get(0);
            ServiceInfo si = r.serviceInfo;
            if (si != null) {
                String pn = si.packageName;
                try {
                    res = context.getPackageManager()
                            .getPackageInfo(pn, 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    public static String getVersionName(Context context) {
        Intent intent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
        intent.setPackage(PACKAGE_NAME);
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentServices(intent, 0);
        String res = null;
        if (!resInfo.isEmpty()) {
            ResolveInfo r = resInfo.get(0);
            ServiceInfo si = r.serviceInfo;
            if (si != null) {
                String pn = si.packageName;
                try {
                    res = context.getPackageManager()
                            .getPackageInfo(pn, 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }

    public static Set<String> allPackageNames() {
        HashSet<String> res = new HashSet<>();
        res.add(PACKAGE_NAME);
        res.add("org.sufficientlysecure.keychain.debug");
        return res;
    }

    public static void doPanicExit(Context ctx) {
        try {
            Intent panicButtonIntent = new Intent("info.guardianproject.panic.action.TRIGGER");
            panicButtonIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            panicButtonIntent.setPackage(PACKAGE_NAME);
            ctx.startActivity(panicButtonIntent);
        } catch (Exception ex) {
            //ignore, probably not installed
        }
    }

    public static boolean isGooglePlayInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.android.vending", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void openInPlayStore(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME));
            activity.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void openInFdroid(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.f-droid.org/app/" + PACKAGE_NAME));
            activity.startActivity(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}

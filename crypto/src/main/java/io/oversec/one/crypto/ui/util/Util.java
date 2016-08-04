package io.oversec.one.crypto.ui.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.view.View;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Util {

    public static final String EXTRA_PACKAGE_NAME = "packagename";
    public static final String EXTRA_ACTIVITY_NAME = "activityname";


    public static void share(final Activity src, final Intent srcIntent, final Intent callbackIntent, String title, IPackageNameFilter filter, final boolean finishActivity) {
        PackageManager pm = src.getPackageManager();

        final MaterialTitleBodyAdapter adapter = new MaterialTitleBodyAdapter(src);
        final List<ResolveInfo> resInfo = pm.queryIntentActivities(srcIntent, 0);
        final List<ResolveInfo> resInfoFiltered = new ArrayList<>();
        for (int i = 0; i < resInfo.size(); i++) {
            // Extract the label, append it, and repackage it in a LabeledIntent
            ResolveInfo ri = resInfo.get(i);
            String packageName = ri.activityInfo.packageName;


            if (!filter.include(packageName)) {
                //don't want it
                continue;
            }
            resInfoFiltered.add(ri);
            adapter.add(new MaterialTitleBodyListItem.Builder(src)
                    .title(ri.loadLabel(pm))
                    //.body(R.string.action_createkey_random_body)
                    .icon(ri.loadIcon(pm))
                    .backgroundColor(Color.WHITE)
                    .build());


        }


        new MaterialDialog.Builder(src)
                .title(title)
                .adapter(adapter, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        dialog.dismiss();
                        ResolveInfo ri = resInfoFiltered.get(which);
                        Intent intent;
                        if (callbackIntent != null) {
                            intent = new Intent(callbackIntent);
                            intent.putExtra(EXTRA_PACKAGE_NAME, ri.activityInfo.packageName);
                            intent.putExtra(EXTRA_ACTIVITY_NAME, ri.activityInfo.name);
                        } else {
                            intent = new Intent(srcIntent);
                            intent.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        }
                        src.startActivity(intent);
                        if (finishActivity) {
                            src.finish();
                        }

                    }
                })
                .cancelable(true)

                .show();


//
//        Intent openInChooser = Intent.createChooser(new Intent(Intent.ACTION_SEND), title);
//
//        List<ResolveInfo> resInfo = pm.queryIntentActivities(srcIntent, 0);
//        List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();
//        for (int i = 0; i < resInfo.size(); i++) {
//            // Extract the label, append it, and repackage it in a LabeledIntent
//            ResolveInfo ri = resInfo.get(i);
//            String packageName = ri.activityInfo.packageName;
//
//
//            if (!filter.include(packageName)) {
//                //don't want it
//                continue;
//            }
//
//            Intent intent = null;
//            if (callbackIntent != null) {
//                intent = new Intent(callbackIntent);
//                intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
//                intent.putExtra(EXTRA_ACTIVITY_NAME, ri.activityInfo.name);
//            } else {
//                intent = new Intent(srcIntent);
//                intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
//            }
//            intentList.add(new LabeledIntent(intent, packageName, "XXXX"+ri.loadLabel(pm), ri.getIconResource()));
//
//        }
//
//        // convert intentList to array
//        LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
//
//        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
//        src.startActivity(openInChooser);
    }

    private interface IPackageNameFilter {
        boolean include(String packageName);
    }


    public static void share(final Activity src, Intent srcIntent, Intent callbackIntent, String title, final boolean excludeOwnPackage, final Set<String> excludeOtherPackages, boolean finishActivity) {
        share(src, srcIntent, callbackIntent, title, new IPackageNameFilter() {
            @Override
            public boolean include(String packageName) {
                //noinspection SimplifiableIfStatement
                if (excludeOwnPackage && src.getPackageName().equals(packageName)) {
                    //not including our own intents
                    return false;
                }
                return !(excludeOtherPackages != null && excludeOtherPackages.contains(packageName));
            }
        }, finishActivity);

    }

    public static void share(Activity src, Intent srcIntent, Intent callbackIntent, final String title, final String onlyThisPackageName, boolean finishActivity) {

        share(src, srcIntent, callbackIntent, title, new IPackageNameFilter() {
            @Override
            public boolean include(String packageName) {
                return onlyThisPackageName == null || onlyThisPackageName.equals(packageName);
            }
        }, finishActivity);

    }

    public static void showToast(Context ctx, String s) {
        Toast.makeText(ctx, s, Toast.LENGTH_LONG).show();
    }

}

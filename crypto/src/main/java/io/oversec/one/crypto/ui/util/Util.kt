package io.oversec.one.crypto.ui.util

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.hardware.Camera
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog

import java.io.IOException
import java.util.ArrayList

object Util {

    const val EXTRA_PACKAGE_NAME = "packagename"
    const val EXTRA_ACTIVITY_NAME = "activityname"


    fun share(
        src: Activity,
        srcIntent: Intent,
        callbackIntent: Intent?,
        title: String,
        filter: IPackageNameFilter,
        finishActivity: Boolean
    ) {
        val pm = src.packageManager

        val adapter = MaterialTitleBodyAdapter(src)
        val resInfo = pm.queryIntentActivities(srcIntent, 0)
        val resInfoFiltered = ArrayList<ResolveInfo>()
        for (i in resInfo.indices) {
            // Extract the label, append it, and repackage it in a LabeledIntent
            val ri = resInfo[i]
            val packageName = ri.activityInfo.packageName


            if (!filter.include(packageName)) {
                //don't want it
                continue
            }
            resInfoFiltered.add(ri)
            adapter.add(
                MaterialTitleBodyListItem.Builder(src)
                    .title(ri.loadLabel(pm))
                    //.body(R.string.action_createkey_random_body)
                    .icon(ri.loadIcon(pm))
                    .backgroundColor(Color.WHITE)
                    .build()
            )
        }


        MaterialDialog.Builder(src)
            .title(title)
            .adapter(adapter) { dialog, itemView, which, text ->
                dialog.dismiss()
                val ri = resInfoFiltered[which]
                val intent: Intent
                if (callbackIntent != null) {
                    intent = Intent(callbackIntent)
                    intent.putExtra(EXTRA_PACKAGE_NAME, ri.activityInfo.packageName)
                    intent.putExtra(EXTRA_ACTIVITY_NAME, ri.activityInfo.name)
                } else {
                    intent = Intent(srcIntent)
                    intent.component =
                            ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                }
                src.startActivity(intent)
                if (finishActivity) {
                    src.finish()
                }
            }
            .cancelable(true)

            .show()


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


    interface IPackageNameFilter {
        fun include(packageName: String): Boolean
    }

    fun share(
        src: Activity,
        srcIntent: Intent,
        callbackIntent: Intent?,
        title: String,
        excludeOwnPackage: Boolean,
        excludeOtherPackages: Set<String>?,
        finishActivity: Boolean
    ) {
        share(src, srcIntent, callbackIntent, title, object : IPackageNameFilter {
            override fun include(packageName: String): Boolean {

                return if (excludeOwnPackage && src.packageName == packageName) {
                    //not including our own intents
                    false
                } else !(excludeOtherPackages != null && excludeOtherPackages.contains(
                    packageName
                ))
            }
        }, finishActivity)

    }

    fun share(
        src: Activity,
        srcIntent: Intent,
        callbackIntent: Intent,
        title: String,
        onlyThisPackageName: String?,
        finishActivity: Boolean
    ) {

        share(src, srcIntent, callbackIntent, title, object : IPackageNameFilter {
            override fun include(packageName: String): Boolean {
                return onlyThisPackageName == null || onlyThisPackageName == packageName
            }
        }, finishActivity)

    }

    fun showToast(ctx: Context, s: String) {
        Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()
    }


    fun checkCameraAccess(ctx: Context): Boolean {
        var ret = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED


        if (ret) {
            //we still can't be sure, android misreport permission when revoked through settings
            var aCamera: Camera? = null
            try {
                aCamera = Camera.open()
                val mParameters = aCamera!!.parameters
                aCamera.parameters = mParameters
            } catch (e: Exception) {
                ret = false
            }

            aCamera?.release()
        }
        return ret
    }


    fun checkExternalStorageAccess(ctx: Context, e: IOException): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED && e.message!!.contains("App op not allowed")
    }

}

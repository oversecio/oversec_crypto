package io.oversec.one.common

import android.app.Activity
import android.app.Fragment
import io.oversec.one.crypto.AbstractEncryptionParams
import io.oversec.one.crypto.BaseDecryptResult
import io.oversec.one.crypto.encoding.pad.PadderContent

abstract class CoreContract {

    abstract val allPaddersSorted: List<PadderContent>

    abstract fun doIfFullVersionOrShowPurchaseDialog(
        activity: Activity,
        okRunnable: Runnable,
        requestCode: Int
    )

    abstract fun doIfFullVersionOrShowPurchaseDialog(
        fragment: Fragment,
        okRunnable: Runnable,
        requestCode: Int
    )

    abstract fun getBestEncryptionParams(packageName: String): AbstractEncryptionParams

    abstract fun isDbSpreadInvisibleEncoding(packagename: String): Boolean

    abstract fun clearEncryptionCache()

    abstract fun putInEncryptionCache(encText: String, r: BaseDecryptResult)

    abstract fun getFromEncryptionCache(encText: String): BaseDecryptResult?

    companion object {

        lateinit var instance: CoreContract
            private set

        fun init(impl: CoreContract) {
            instance = impl
        }
    }
}

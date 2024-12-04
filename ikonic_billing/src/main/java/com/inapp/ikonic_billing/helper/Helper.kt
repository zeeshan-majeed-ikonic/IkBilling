package com.inapp.ikonic_billing.helper

import android.util.Log
import com.inapp.ikonic_billing.helper.IkBillingInfo.enableLoging

const val TAG = "IKBillingLogs"
const val IK_PURCHASES_DB = "IK_PURCHASES_DB"
const val IK_BILLING_PREF = "IK_BILLING_PREF"
const val KEY_IS_PREMIUM = "KEY_IS_PREMIUM"

fun logger(message: String, isError: Boolean = false) {

    if (enableLoging) {
        if (isError) {
            Log.e(TAG, message)
            return
        }
        Log.d(TAG, message)
    }
}
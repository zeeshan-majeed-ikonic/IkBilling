package com.inapp.ikonic_billing.billingDb

import android.content.Context
import android.os.Build
import com.inapp.ikonic_billing.helper.IK_BILLING_PREF
import com.inapp.ikonic_billing.helper.KEY_IS_PREMIUM

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkBillingPreferences(context: Context) {
    private val sharedPreferences = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(IK_BILLING_PREF, Context.MODE_PRIVATE)
    } else {
        context.getSharedPreferences(IK_BILLING_PREF, Context.MODE_PRIVATE)
    }

    fun setPremiumUser(isPremium: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PREMIUM, isPremium)
            .apply()
    }

    fun isPremiumUser(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PREMIUM, false)
    }
}
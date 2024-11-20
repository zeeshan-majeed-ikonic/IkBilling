package com.inapp.ikonic_billing.interfaces

import com.android.billingclient.api.Purchase
import com.inapp.ikonic_billing.enum_errors.IkBillingErrors

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

interface IkBillingEventListeners {
    fun onIkProductsPurchased(purchases: List<Purchase?>)
    fun onIkPurchaseAcknowledged(purchase: Purchase)
    fun onIkPurchaseConsumed(purchase: Purchase)
    fun onIkBillingError(error: IkBillingErrors)
}
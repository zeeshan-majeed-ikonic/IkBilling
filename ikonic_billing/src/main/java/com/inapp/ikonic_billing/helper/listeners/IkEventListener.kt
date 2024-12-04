package com.inapp.ikonic_billing.helper.listeners

import com.android.billingclient.api.Purchase
import com.inapp.ikonic_billing.enum_class.IkBillingErrors

interface IkEventListener {
    fun onProductsPurchased(purchases: List<Purchase?>)
    fun onPurchaseAcknowledged(purchase: Purchase)
    fun onPurchaseConsumed(purchase: Purchase)
    fun onBillingError(error: IkBillingErrors)
}
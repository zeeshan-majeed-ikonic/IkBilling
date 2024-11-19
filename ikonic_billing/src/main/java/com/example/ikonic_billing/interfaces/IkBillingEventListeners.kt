package com.example.ikonic_billing.interfaces

import com.android.billingclient.api.Purchase
import com.example.ikonic_billing.enum_errors.IkBillingErrors

interface IkBillingEventListeners {
    fun onIkProductsPurchased(purchases: List<Purchase?>)
    fun onIkPurchaseAcknowledged(purchase: Purchase)
    fun onIkPurchaseConsumed(purchase: Purchase)
    fun onIkBillingError(error: IkBillingErrors)
}
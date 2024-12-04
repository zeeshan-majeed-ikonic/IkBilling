package com.inapp.ikonic_billing.helper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.inapp.ikonic_billing.billingDb.IkPurchasedProduct
import com.inapp.ikonic_billing.helper.listeners.IkClientListener
import com.inapp.ikonic_billing.helper.listeners.IkEventListener

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

object IkBillingInfo {
    var enableLoging = false
    var isClientReady = false
    var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    val subProductIds by lazy { mutableListOf<String>() }
    val inAppProductIds by lazy { mutableListOf<String>() }
    var billingClient: BillingClient? = null
    var ikEventListener: IkEventListener? = null
    var ikClientListener: IkClientListener? = null
    val allIkProducts by lazy { mutableListOf<ProductDetails>() }
    val consumeAbleProductIds by lazy { mutableListOf<String>() }
    val purchasedSubsProductList by lazy { mutableListOf<Purchase>() }
    val purchasedInAppProductList by lazy { mutableListOf<Purchase>() }
    var lastIkPurchasedProduct: IkPurchasedProduct? = null
}
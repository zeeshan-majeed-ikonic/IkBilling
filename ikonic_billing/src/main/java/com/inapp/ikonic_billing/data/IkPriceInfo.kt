package com.inapp.ikonic_billing.data

import androidx.annotation.Keep
import com.android.billingclient.api.ProductDetails

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

@Keep
data class IkPriceInfo(
    var productId: String = "",
    var basePlanId: String = "",
    var offerId: String = "",
    var title: String = "",
    var type: String = "",
    var duration: String = "",
    var price: String = "",
    var priceMicro: Long = 0L,
    var currencyCode: String = "",
    var productCompleteInfo: ProductDetails? = null
)

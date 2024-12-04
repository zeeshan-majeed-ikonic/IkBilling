package com.inapp.ikonic_billing.billingDb

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

@Keep
@Entity(tableName = "billed_products")
data class IkPurchasedProduct (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "orderId") var orderId: String? = null,
    @ColumnInfo(name = "productId") var productId: String? = null,
    @ColumnInfo(name = "base_plan_id") var basePlanId: String? = null,
    @ColumnInfo(name = "offer_id") var offerId: String? = null,
    @ColumnInfo(name = "title") var title: String? = null,
    @ColumnInfo(name = "type") var type: String? = null,
    @ColumnInfo(name = "duration") var duration: String? = null,
    @ColumnInfo(name = "price") var price: String? = null,
    @ColumnInfo(name = "price_micro") var priceMicro: Long? = null,
    @ColumnInfo(name = "currency_code") var currencyCode: String? = null,
    @ColumnInfo(name = "purchase_time") var purchaseTime: Long = System.currentTimeMillis(),
)
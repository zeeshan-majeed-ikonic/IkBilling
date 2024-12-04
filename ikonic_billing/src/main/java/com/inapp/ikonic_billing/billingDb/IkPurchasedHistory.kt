package com.inapp.ikonic_billing.billingDb

import android.content.Context
import com.inapp.ikonic_billing.helper.logger

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkPurchasedHistory(private val context: Context) {
    suspend fun recordPurchase(purchase: IkPurchasedProduct) {
        val db = IkBillingDB.getDatabase(context)
        db.purchaseDao().insertPurchasedProduct(purchase)
    }

    suspend fun hasUserEverPurchased(): Boolean {
        val db = IkBillingDB.getDatabase(context)
        val products = db.purchaseDao().getAllPurchasedProducts()
        products.forEach {
            logger("IkPurchasedHistory = $it")
        }
        return db.purchaseDao().getAllPurchasedProducts().isNotEmpty()
    }

    suspend fun getPurchasedPlansHistory(): List<IkPurchasedProduct> {
        val db = IkBillingDB.getDatabase(context)
        return db.purchaseDao().getAllPurchasedProducts()
    }
}
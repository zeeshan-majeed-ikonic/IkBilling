package com.inapp.ikonic_billing.billingDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BillingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasedProduct(purchase: IkPurchasedProduct)

    @Query("SELECT * FROM billed_products")
    suspend fun getAllPurchasedProducts(): List<IkPurchasedProduct>
}
package com.inapp.ikonic_billing.billingDb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.inapp.ikonic_billing.helper.IK_PURCHASES_DB

@Database(entities = [IkPurchasedProduct::class], version = 1, exportSchema = false)
abstract class IkBillingDB : RoomDatabase() {
    abstract fun purchaseDao(): BillingDao

    companion object {
        @Volatile
        private var INSTANCE: IkBillingDB? = null

        fun getDatabase(context: Context): IkBillingDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IkBillingDB::class.java,
                    IK_PURCHASES_DB
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
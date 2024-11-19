package com.example.ikonicappsbilling

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MyBillingUtils.startBilling(this)

        getAllPrices()
    }

    companion object{
        private const val TAG = "my_billing"
    }

    private fun getAllPrices(){
        MyBillingUtils.apply {
            Log.i(TAG, "InApp price: ${getInAppPrice(this@MainActivity)}")
            Log.i(TAG, "Sub price: ${getSubsPrice(this@MainActivity)}")

        }
    }
}
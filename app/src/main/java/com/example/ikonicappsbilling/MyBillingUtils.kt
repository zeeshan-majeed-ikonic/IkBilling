package com.example.ikonicappsbilling

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.inapp.ikonic_billing.enum_errors.IkBillingErrors
import com.inapp.ikonic_billing.helper.IkBillingHelper
import com.inapp.ikonic_billing.interfaces.IkBillingClientListeners
import com.inapp.ikonic_billing.interfaces.IkBillingEventListeners

object MyBillingUtils {

    val isPremiumUser = MutableLiveData(false)

    fun startBilling(context: Context) {
        val ikBillingHelper = IkBillingHelper(context)
        ikBillingHelper.setInAppKeys(mutableListOf("your-inapp-id"))
            .setSubKeys(mutableListOf("your-product-id"))
            .enableIkLogs(true)
            .setBillingClientListener(object : IkBillingClientListeners {
                override fun onIkPurchasesUpdated() {
                    isPremiumUser.value =
                        ikBillingHelper.isSubsPremiumUser() || ikBillingHelper.isInAppPremiumUser()

                }

                override fun onIkClientReady() {
                    isPremiumUser.value =
                        ikBillingHelper.isSubsPremiumUser() || ikBillingHelper.isInAppPremiumUser()

                }

                override fun onIkClientInitError() {

                }

            }).initIkBilling()
        attachIkBillingListeners(ikBillingHelper)
    }

    fun getSubsPrice(context: Context): String {
        return IkBillingHelper(context)
            .getProductPriceByKey("subscription-key", "")?.price ?: ""
    }
    fun getSubsPriceWithOffer(context: Context): String {
        return IkBillingHelper(context)
            .getProductPriceByKey("subscription-key", "offer-key")?.price ?: ""
    }

    fun getInAppPrice(context: Context): String {
        return IkBillingHelper(context).getProductPriceByKey("inapp-key")?.price ?: ""
    }

    fun subscribe(activity: Activity){
        IkBillingHelper(activity).subscribeIkProduct(activity,"product-key","")
    }

    fun subscribeWithOffer(activity: Activity){
        IkBillingHelper(activity).subscribeIkProduct(activity,"product-key","offer-key")
    }

    fun buyInApp(activity: Activity){
        IkBillingHelper(activity).buyIkInApp(activity,"inapp-key",false)
    }
    private fun attachIkBillingListeners(ikBillingHelper: IkBillingHelper) {
        ikBillingHelper.setBillingEventListener(object : IkBillingEventListeners {
            override fun onIkProductsPurchased(purchases: List<Purchase?>) {
                isPremiumUser.value =
                    ikBillingHelper.isSubsPremiumUser() || ikBillingHelper.isInAppPremiumUser()
            }

            override fun onIkPurchaseAcknowledged(purchase: Purchase) {
                isPremiumUser.value =
                    ikBillingHelper.isSubsPremiumUser() || ikBillingHelper.isInAppPremiumUser()

            }

            override fun onIkPurchaseConsumed(purchase: Purchase) {

            }

            override fun onIkBillingError(error: IkBillingErrors) {
                when (error) {
                    IkBillingErrors.DEVELOPER_ERROR -> {

                    }

                    IkBillingErrors.CLIENT_NOT_READY -> {

                    }

                    IkBillingErrors.CLIENT_DISCONNECTED -> {

                    }

                    IkBillingErrors.PRODUCT_NOT_EXIST -> {

                    }

                    IkBillingErrors.OFFER_NOT_EXIST -> {

                    }

                    IkBillingErrors.BILLING_ERROR -> {

                    }

                    IkBillingErrors.USER_CANCELED -> {

                    }

                    IkBillingErrors.SERVICE_UNAVAILABLE -> {

                    }

                    IkBillingErrors.BILLING_UNAVAILABLE -> {

                    }

                    IkBillingErrors.ITEM_UNAVAILABLE -> {

                    }

                    IkBillingErrors.ERROR -> {

                    }

                    IkBillingErrors.ITEM_ALREADY_OWNED -> {

                    }

                    IkBillingErrors.ITEM_NOT_OWNED -> {

                    }

                    IkBillingErrors.SERVICE_DISCONNECTED -> {

                    }

                    IkBillingErrors.ACKNOWLEDGE_ERROR -> {

                    }

                    IkBillingErrors.ACKNOWLEDGE_WARNING -> {

                    }

                    IkBillingErrors.OLD_PURCHASE_TOKEN_NOT_FOUND -> {

                    }

                    IkBillingErrors.INVALID_PRODUCT_TYPE_SET -> {

                    }

                    IkBillingErrors.CONSUME_ERROR -> {

                    }
                }
            }
        })
    }
}
package com.example.ikonicappsbilling

import android.app.Activity
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.Purchase
import com.inapp.ikonic_billing.IKBillingUtils
import com.inapp.ikonic_billing.enum_class.IkBillingErrors
import com.inapp.ikonic_billing.helper.listeners.IkClientListener
import com.inapp.ikonic_billing.helper.listeners.IkEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MyBillingUtils {

    val isPremiumUser = MutableLiveData(false)

    fun startBilling(context: Context) {
        val ikBillingHelper = IKBillingUtils(context)
        ikBillingHelper.setInAppProductKeys(mutableListOf("your-inapp-id"))
            .setSubProductKeys(mutableListOf("your-product-id"))
            .enableLogger(true)
            .setIkClientListener(object : IkClientListener {
                override fun onPurchasesUpdated() {
                   CoroutineScope(Dispatchers.Main).launch {
                       isPremiumUser.value = (ikBillingHelper.isHavingSubscription()
                               || ikBillingHelper.isHavingInApp()
                               || ikBillingHelper.isPremiumUser)
                   }
                }

                override fun onClientReady() {
                    CoroutineScope(Dispatchers.Main).launch {
                        isPremiumUser.value = (ikBillingHelper.isHavingSubscription()
                                || ikBillingHelper.isHavingInApp()
                                || ikBillingHelper.isPremiumUser)
                    }
                }

                override fun onClientInitError() {
                }


            }).initIkBilling()
        attachIkBillingListeners(ikBillingHelper)
    }

    fun getSubsPrice(context: Context): String {
        return IKBillingUtils(context)
            .getIkSubscriptionProductPriceById("subscription-key")?.price ?: ""
    }

    fun getSubsPriceWithOffer(context: Context): String {
        return IKBillingUtils(context)
            .getIkSubscriptionProductPriceById("subscription-key", "offer-key")?.price ?: ""
    }

    fun getInAppPrice(context: Context): String {
        return IKBillingUtils(context).getIkInAppProductPriceById("inapp-key")?.price ?: ""
    }

    fun subscribe(activity: Activity) {
        IKBillingUtils(activity).subscribeIkProduct(activity, "product-key", "")
    }

    fun subscribeWithOffer(activity: Activity) {
        IKBillingUtils(activity).subscribeIkProduct(activity, "product-key", "offer-key")
    }

    fun buyInApp(activity: Activity) {
        IKBillingUtils(activity).buyIkInApp(activity, "inapp-key", false)
    }

    private fun attachIkBillingListeners(ikBillingHelper: IKBillingUtils) {
        ikBillingHelper.setIkEventListener(object : IkEventListener {
            override fun onProductsPurchased(purchases: List<Purchase?>) {
                CoroutineScope(Dispatchers.Main).launch {
                    isPremiumUser.value = (ikBillingHelper.isHavingSubscription()
                            || ikBillingHelper.isHavingInApp()
                            || ikBillingHelper.isPremiumUser)
                }
            }

            override fun onPurchaseAcknowledged(purchase: Purchase) {
                CoroutineScope(Dispatchers.Main).launch {
                    isPremiumUser.value = (ikBillingHelper.isHavingSubscription()
                            || ikBillingHelper.isHavingInApp()
                            || ikBillingHelper.isPremiumUser)
                }
            }

            override fun onPurchaseConsumed(purchase: Purchase) {

            }

            override fun onBillingError(error: IkBillingErrors) {
                when (error) {
                    IkBillingErrors.DEVELOPER_ERROR -> {}
                    IkBillingErrors.PRODUCT_NOT_EXIST -> {}
                    IkBillingErrors.OFFER_NOT_EXIST -> {}
                    IkBillingErrors.USER_CANCELED -> {}
                    IkBillingErrors.SERVICE_UNAVAILABLE -> {}
                    IkBillingErrors.BILLING_UNAVAILABLE -> {}
                    IkBillingErrors.ITEM_UNAVAILABLE -> {}
                    IkBillingErrors.ERROR -> {}
                    IkBillingErrors.ITEM_ALREADY_OWNED -> {}
                    IkBillingErrors.SERVICE_DISCONNECTED -> {}
                    IkBillingErrors.ACKNOWLEDGE_ERROR -> {}
                    IkBillingErrors.ACKNOWLEDGE_WARNING -> {}
                    IkBillingErrors.OLD_PURCHASE_TOKEN_NOT_FOUND -> {}
                    IkBillingErrors.CONSUME_ERROR -> {}
                    else -> {}
                }
            }
        })
    }
}
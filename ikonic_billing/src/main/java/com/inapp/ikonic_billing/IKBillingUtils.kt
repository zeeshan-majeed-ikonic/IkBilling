package com.inapp.ikonic_billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.inapp.ikonic_billing.billingDb.IkBillingPreferences
import com.inapp.ikonic_billing.billingDb.IkPurchasedHistory
import com.inapp.ikonic_billing.billingDb.IkPurchasedProduct
import com.inapp.ikonic_billing.helper.IkBillingInfo.allIkProducts
import com.inapp.ikonic_billing.helper.IkBillingInfo.billingClient
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikClientListener
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikEventListener
import com.inapp.ikonic_billing.helper.IkBillingInfo.consumeAbleProductIds
import com.inapp.ikonic_billing.helper.IkBillingInfo.enableLoging
import com.inapp.ikonic_billing.helper.IkBillingInfo.inAppProductIds
import com.inapp.ikonic_billing.helper.IkBillingInfo.isClientReady
import com.inapp.ikonic_billing.helper.IkBillingInfo.lastIkPurchasedProduct
import com.inapp.ikonic_billing.helper.IkBillingInfo.purchasedInAppProductList
import com.inapp.ikonic_billing.helper.IkBillingInfo.purchasedSubsProductList
import com.inapp.ikonic_billing.helper.IkBillingInfo.purchasesUpdatedListener
import com.inapp.ikonic_billing.helper.IkBillingInfo.subProductIds
import com.inapp.ikonic_billing.helper.IkStartPurchasing
import com.inapp.ikonic_billing.helper.IkProductDetail
import com.inapp.ikonic_billing.helper.IkProductPrices
import com.inapp.ikonic_billing.helper.logger
import com.inapp.ikonic_billing.data.IkPriceInfo
import com.inapp.ikonic_billing.enum_class.IkBillingErrors
import com.inapp.ikonic_billing.helper.listeners.IkClientListener
import com.inapp.ikonic_billing.helper.listeners.IkEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IKBillingUtils(private val context: Context) {

    private val ikProductPrices = IkProductPrices()
    private val ikProductDetail = IkProductDetail()
    private val ikStartPurchasing = IkStartPurchasing()
    private val ikPurchasedHistory = IkPurchasedHistory(context)
    private val ikBillingPreferences = IkBillingPreferences(context)

    private fun startConnection() {
        logger("Connection started")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    logger("Connected..!")
                    isClientReady = true

                    CoroutineScope(Main).launch {
                        val subsDeferred = CompletableDeferred<Unit>()
                        val inAppDeferred = CompletableDeferred<Unit>()
                        withContext(IO) {
                            if (subProductIds.isNotEmpty()) {
                                fetchAvailableAllSubsProducts(subProductIds, subsDeferred)
                            } else {
                                subsDeferred.complete(Unit)
                            }

                            if (inAppProductIds.isNotEmpty()) {
                                fetchAvailableAllInAppProducts(inAppProductIds, inAppDeferred)
                            } else {
                                inAppDeferred.complete(Unit)
                            }
                        }
                        awaitAll(subsDeferred, inAppDeferred)
                        val purchasesDeferred = CompletableDeferred<Unit>()
                        withContext(IO) {
                            fetchAndUpdateActivePurchases(purchasesDeferred)
                        }
                        purchasesDeferred.await()
                        logger("Client is ready")
                        updatePremiumStatus(context = context)
                        ikClientListener?.onClientReady()
                    }

                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    ikClientListener?.onPurchasesUpdated()
                }
            }

            override fun onBillingServiceDisconnected() {
                logger("Connection failed..!",true)
                isClientReady = false
                // callback with Main thread because billing throw it in IO thread
                CoroutineScope(Main).launch {
                    ikClientListener?.onClientInitError()
                }
            }
        })
    }

    private fun fetchAvailableAllSubsProducts(productIds: MutableList<String>, subsDeferred: CompletableDeferred<Unit>) {
        // Early return if billing client is null
        val client = billingClient ?: run {
            logger("Billing client empty during fetching All Subscription Products", true)
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            subsDeferred.complete(Unit)
            return
        }

        val productList = productIds.map {
            logger("Sub Id: $it")
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { productDetails ->
                    logger("Sub details: $productDetails")
                    allIkProducts.add(productDetails)
                }
            } else {
                logger("Failed to find Sub prices: ${billingResult.debugMessage}",true)
            }

            subsDeferred.complete(Unit)
        }
    }

    private fun fetchAvailableAllInAppProducts(productIds: MutableList<String>, inAppDeferred: CompletableDeferred<Unit>) {
        val client = billingClient ?: run {
            logger("Client empty during fetching All In-App", true)
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            inAppDeferred.complete(Unit)
            return
        }
        val productList = productIds.map {
            logger("In-App Id: $it")
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { productDetails ->
                    logger("In-app details: $productDetails")
                    allIkProducts.add(productDetails)
                }
            } else {
                logger("Failed to get In-APP prices: ${billingResult.debugMessage}",true)
            }
            inAppDeferred.complete(Unit)
        }
    }

    private fun fetchAndUpdateActivePurchases(purchasesDeferred: CompletableDeferred<Unit>) {
        val billingClient = billingClient ?: run {
            logger("Client is empty during fetching active purchases", true)
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            purchasesDeferred.complete(Unit)
            return
        }
        // Atomic counter to track the completion of both purchase queries
        val pendingQueries = AtomicInteger(2)

        // Helper function to query purchases and handle results
        fun queryAndHandlePurchases(productType: String) {
            val params = QueryPurchasesParams.newBuilder().setProductType(productType).build()
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val activePurchases = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    logger("$productType purchases found: ${activePurchases.size}")

                    activePurchases.forEach { purchase ->
                        logger("$productType purchase: ${purchase.products.first()}")
                        ikStartPurchasing.handleIkPurchase(purchase)
                    }
                } else {
                    logger("No $productType purchases found",true)
                }
                // Complete the deferred once both queries are done
                if (pendingQueries.decrementAndGet() == 0) {
                    purchasesDeferred.complete(Unit)
                }
            }
        }
        // Start both queries
        queryAndHandlePurchases(BillingClient.ProductType.SUBS)
        queryAndHandlePurchases(BillingClient.ProductType.INAPP)
    }

    fun setSubProductKeys(productIds: MutableList<String>): IKBillingUtils {
        subProductIds.addAll(productIds)
        return this
    }

    fun initIkBilling() {
        if (billingClient == null) {
            isClientReady = false
            logger("Setup new Connection")
            purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK                                                                      -> {
                        purchases?.let {
                            for (purchase in it) {
                                logger("purchased --> $purchase")
                                logger("purchased First --> ${purchase.products.first()}")
//								val productPriceInfo = getAllProductPrices().find { it.productId == purchase }
                                CoroutineScope(IO).launch {
                                    lastIkPurchasedProduct?.let { originalProduct ->
                                        val updatedProduct = originalProduct.copy(orderId = purchase.orderId)
                                        ikPurchasedHistory.recordPurchase(purchase = updatedProduct)
                                    }
                                    ikStartPurchasing.handleIkPurchase(purchase = purchase)
                                }
                            }
                            ikEventListener?.onProductsPurchased(purchasedSubsProductList)
                        }
                    }

                    BillingClient.BillingResponseCode.USER_CANCELED                                                           -> {
                        logger("User pressed back or canceled a dialog." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.USER_CANCELED)
                    }

                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE                                                     -> {
                        logger("Network connection is down." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.SERVICE_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE                                                     -> {
                        logger("Billing API version is not supported for the type requested." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.BILLING_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE                                                        -> {
                        logger("Requested product is not available for purchase." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.ITEM_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.DEVELOPER_ERROR                                                         -> {
                        logger("Invalid arguments provided to the API." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.DEVELOPER_ERROR)

                    }

                    BillingClient.BillingResponseCode.ERROR                                                                   -> {
                        logger("Fatal error during the API action." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.ERROR)
                    }

                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED                                                      -> {
                        logger("Failure to purchase since item is already owned." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.ITEM_ALREADY_OWNED)
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED                                                          -> {
                        logger("Failure to consume since item is not owned." + " Response code: " + billingResult.responseCode)
                        ikEventListener?.onBillingError(IkBillingErrors.ITEM_NOT_OWNED)
                    }

                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                        logger("Initialization error: service disconnected/timeout. Trying to reconnect...")
                        ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
                    }

                    else                                                                                                      -> {
                        logger("Initialization error: ")
                        ikEventListener?.onBillingError(IkBillingErrors.ERROR)
                    }
                }
            }
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener!!)
                .enablePendingPurchases().build()
            startConnection()
        } else {
            ikClientListener?.onClientAllReadyConnected()
//            logFunsolBilling("Client already connected")
        }
    }

    fun subscribeIkProduct(activity: Activity, basePlanId: String, offerId: String? = null) {
        ikStartPurchasing.subscribeIkProduct(activity = activity, basePlanId = basePlanId, offerId = offerId)
    }

    private fun upgradeOrDowngradeSubscription(
        activity: Activity,
        updateProductId: String,
        updateOfferId: String,
        oldProductID: String,
        policy: Int
    ) {
        if (billingClient != null) {
            val productInfo = ikProductDetail.getIkProductDetail(updateProductId, updateOfferId, BillingClient.ProductType.SUBS)
            if (productInfo != null) {
                val oldToken = getOldPurchaseToken(oldProductID)
                if (oldToken.trim().isNotEmpty()) {
                    val productDetailsParamsList =
                        ArrayList<BillingFlowParams.ProductDetailsParams>()
                    if (productInfo.productType == BillingClient.ProductType.SUBS && productInfo.subscriptionOfferDetails != null) {
                        val offerToken = ikProductDetail.getIkOfferToken(productInfo.subscriptionOfferDetails, updateProductId, updateOfferId)
                        if (offerToken.trim { it <= ' ' } != "") {
                            productDetailsParamsList.add(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productInfo).setOfferToken(offerToken)
                                    .build()
                            )
                        } else {
                            ikEventListener?.onBillingError(IkBillingErrors.OFFER_NOT_EXIST)
                            logger("The offer id: $updateProductId doesn't seem to exist",true)
                            return
                        }
                    } else {
                        productDetailsParamsList.add(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productInfo).build()
                        )
                    }
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .setSubscriptionUpdateParams(
                            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                .setOldPurchaseToken(oldToken)
                                .setSubscriptionReplacementMode(policy)
                                .build()
                        ).build()
                    billingClient!!.launchBillingFlow(activity, billingFlowParams)
                } else {
                    logger("old purchases token not found", true)
                    ikEventListener?.onBillingError(IkBillingErrors.OLD_PURCHASE_TOKEN_NOT_FOUND)

                }
            } else {
                logger("Client can not launch billing flow because product details are missing while update",true)
                ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
            }
        } else {
            logger("Client is empty during Update subs",true)
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
        }
    }

    private fun getOldPurchaseToken(basePlanId: String): String {
        val matchingProduct = allIkProducts.firstOrNull { product ->
            product.productType == BillingClient.ProductType.SUBS && product.subscriptionOfferDetails?.any { it.basePlanId == basePlanId } == true
        }
        matchingProduct?.let { product ->
            val matchingPurchase = purchasedSubsProductList.firstOrNull { purchase ->
                purchase.products.firstOrNull() == product.productId
            }
            return matchingPurchase?.purchaseToken ?: ""
        }
        return ""
    }

    fun isHavingSubscription(): Boolean {
        return purchasedSubsProductList.isNotEmpty()
    }

    fun isSubsPremiumUserByBasePlanId(basePlanId: String): Boolean {
        val isPremiumUser = allIkProducts.any { product ->
            product.productType == BillingClient.ProductType.SUBS &&
                    product.subscriptionOfferDetails?.any { it.basePlanId == basePlanId } == true &&
                    purchasedSubsProductList.any { it.products.firstOrNull() == product.productId }
        }

        if (!isPremiumUser) {
            ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        }

        return isPremiumUser
    }

    fun isSubsPremiumUserBySubProductID(subId: String): Boolean {
        return purchasedSubsProductList.any { it.products.first() == subId }
    }

    fun areSubscriptionsSupported(): Boolean {
        return if (billingClient != null) {
            val responseCode =
                billingClient!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
            responseCode.responseCode == BillingClient.BillingResponseCode.OK
        } else {
            logger("Client empty during check subscription",true)
            ikEventListener?.onBillingError(IkBillingErrors.BILLING_UNAVAILABLE)

            false
        }
    }

    fun unsubscribeIkProduct(activity: Activity, SubId: String) {
        try {
            val subscriptionUrl =
                "http://play.google.com/store/account/subscriptions?package=" + activity.packageName + "&sku=" + SubId
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(subscriptionUrl)
            activity.startActivity(intent)
            activity.finish()
        } catch (e: Exception) {
            logger("error during unsubscribe", true)
            e.printStackTrace()
        }
    }

    fun buyIkInApp(activity: Activity, productId: String, isPersonalizedOffer: Boolean = false) {
        ikStartPurchasing.buyIKInApp(activity = activity, productId = productId, isPersonalizedOffer = isPersonalizedOffer)
    }

    fun isHavingInApp(): Boolean {
        return purchasedInAppProductList.isNotEmpty()
    }

    private fun updatePremiumStatus(context: Context) {
        val isPremium = IKBillingUtils(context).isHavingInApp() || IKBillingUtils(context).isHavingSubscription()
        ikBillingPreferences.setPremiumUser(isPremium = isPremium)
    }

    val isPremiumUser get() = ikBillingPreferences.isPremiumUser()

    fun isInAppPremiumUserByProductId(productId: String): Boolean {
        return purchasedInAppProductList.any { purchase ->
            purchase.products.any { it == productId }
        }
    }

    fun setInAppProductKeys(productIds: MutableList<String>): IKBillingUtils {
        inAppProductIds.addAll(productIds)
        return this
    }

    fun setConsumableProductIds(productIds: MutableList<String>): IKBillingUtils {
        consumeAbleProductIds.addAll(productIds)
        return this
    }

    fun getIkAllProductPrices(): MutableList<IkPriceInfo> {
        return ikProductPrices.getIkAllProductPrices()
    }

    fun getIkSubscriptionProductPriceById(basePlanId: String, offerId: String? = null): IkPriceInfo? {
        return ikProductPrices.getIkSubscriptionProductPriceById(basePlanId = basePlanId, offerId = offerId)
    }

    fun getIkInAppProductPriceById(inAppProductId: String): IkPriceInfo? {
        return ikProductPrices.getIkInAppProductPriceById(inAppProductId = inAppProductId)
    }

    fun enableLogger(isEnableLog: Boolean = true): IKBillingUtils {
        enableLoging = isEnableLog
        return this
    }

    fun isIkClientReady(): Boolean {
        return isClientReady
    }

    fun releaseIkBilling() {
        if (billingClient != null && billingClient!!.isReady) {
            logger("Releasing clint...! ")
            billingClient?.endConnection()
        }
    }

    fun setIkEventListener(ikEventListeners: IkEventListener?): IKBillingUtils {
        ikEventListener = ikEventListeners
        return this
    }

    fun setIkClientListener(ikClientListeners: IkClientListener?): IKBillingUtils {
        ikClientListener = ikClientListeners
        return this
    }

    fun isOfferAvailable(basePlanId: String, offerId: String): Boolean {
        val offerPrice = ikProductPrices.getIkSubscriptionProductPriceById(basePlanId = basePlanId, offerId = offerId)
        return offerPrice != null
    }

    fun wasPremiumUser(): Boolean = runBlocking {
        ikPurchasedHistory.hasUserEverPurchased()
    }

    fun getPurchasedPlansHistory(): List<IkPurchasedProduct> = runBlocking {
        ikPurchasedHistory.getPurchasedPlansHistory()
    }

    fun getInAppProductDetail(productId: String, productType: String): ProductDetails? {
        return ikProductDetail.getIkProductDetail(productId = productId, offerId = null, productType = productType)
    }

    fun getSubscriptionProductDetail(productId: String, offerId: String? = null, productType: String): ProductDetails? {
        return ikProductDetail.getIkProductDetail(productId = productId, offerId = offerId, productType = productType)
    }
}
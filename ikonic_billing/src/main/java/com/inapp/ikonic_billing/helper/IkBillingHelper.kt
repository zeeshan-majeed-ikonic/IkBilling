package com.inapp.ikonic_billing.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.inapp.ikonic_billing.enum_errors.IkBillingErrors
import com.inapp.ikonic_billing.interfaces.IkBillingClientListeners
import com.inapp.ikonic_billing.interfaces.IkBillingEventListeners
import com.inapp.ikonic_billing.model.IkBillingProduct
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkBillingHelper(private val context: Context) {

    companion object {
        private const val TAG = "IkBillingHelper"
        private var isClientReady = false
        private var billingClient: BillingClient? = null
        private var premiumEventListener: IkBillingEventListeners? = null
        private var premiumClientListener: IkBillingClientListeners? = null
        private var purchasesUpdatedListener: PurchasesUpdatedListener? = null

        private val subKeys by lazy { mutableListOf<String>() }
        private val inAppKeys by lazy { mutableListOf<String>() }
        private val consumeAbleKeys by lazy { mutableListOf<String>() }
        private val allProducts by lazy { mutableListOf<ProductDetails>() }
        private val purchasedSubsProductList by lazy { mutableListOf<Purchase>() }
        private val purchasedInAppProductList by lazy { mutableListOf<Purchase>() }

        private var enableLog = false
    }

    fun initIkBilling() {
        if (billingClient == null) {
            isClientReady = false
            ikLogger("init IK billing client")
            purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases?.let {
                            for (purchase in it) {
                                ikLogger("purchases --> $purchase")
                                CoroutineScope(Dispatchers.IO).launch {
                                    handlePurchase(purchase)
                                }
                            }
                            premiumEventListener?.onIkProductsPurchased(purchasedSubsProductList)
                        }
                    }

                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        ikLogger("User pressed back or canceled the dialog." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.USER_CANCELED)
                    }

                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                        ikLogger("Network connection Error." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                        ikLogger("Billing API version is not supported for the type requested." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.BILLING_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                        ikLogger("Requested product is not available for purchase." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.ITEM_UNAVAILABLE)

                    }

                    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                        ikLogger("Invalid arguments provided to the API." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.DEVELOPER_ERROR)

                    }

                    BillingClient.BillingResponseCode.ERROR -> {
                        ikLogger("Fatal error during the API action." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.ERROR)
                    }

                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        ikLogger("Failure to purchase since item is already owned." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.ITEM_ALREADY_OWNED)
                    }

                    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                        ikLogger("Failure to consume since item is not owned." + " Response code: " + billingResult.responseCode)
                        premiumEventListener?.onIkBillingError(IkBillingErrors.ITEM_NOT_OWNED)
                    }

                    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> {
                        ikLogger("Initialization error: service disconnected/timeout. Trying to reconnect...")
                        premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
                    }

                    else -> {
                        ikLogger("Initialization error: ")
                        premiumEventListener?.onIkBillingError(IkBillingErrors.ERROR)
                    }
                }
            }
            purchasesUpdatedListener?.let { listener ->
                billingClient = BillingClient.newBuilder(context)
                    .setListener(listener)
                    .enablePendingPurchases(
                        PendingPurchasesParams
                            .newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    ).build()
            }
            startIkConnection()
        } else {
            premiumClientListener?.onIkClientAllReadyConnected()
        }
    }

    private fun startIkConnection() {

        ikLogger("Connect start with Google Play")
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    ikLogger("Connected to Google Play")
                    isClientReady = true

                    CoroutineScope(Dispatchers.Main).launch {
                        // Define CompletableDeferred for each async task
                        val subsDeferred = CompletableDeferred<Unit>()
                        val inAppDeferred = CompletableDeferred<Unit>()
                        val purchasesDeferred = CompletableDeferred<Unit>()

                        // Fetch subscriptions
                        withContext(Dispatchers.IO) {
                            if (subKeys.isNotEmpty()) {
                                fetchIkAvailableAllSubsProducts(subKeys, subsDeferred)
                            } else {
                                subsDeferred.complete(Unit)
                            }
                        }

                        // Fetch in-app products
                        withContext(Dispatchers.IO) {
                            if (inAppKeys.isNotEmpty()) {
                                fetchIkAvailableAllInAppProducts(inAppKeys, inAppDeferred)
                            } else {
                                inAppDeferred.complete(Unit)
                            }
                        }

                        // Fetch active purchases
                        withContext(Dispatchers.IO) {
                            fetchActivePurchases(purchasesDeferred)
                        }

                        // Await all CompletableDeferred to complete
                        awaitAll(subsDeferred, inAppDeferred, purchasesDeferred)

                        // Notify the listener on the Main thread
                        withContext(Dispatchers.Main) {
                            ikLogger("Billing client is ready")
                            premiumClientListener?.onIkClientReady()
                        }
                    }

                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    premiumClientListener?.onIkPurchasesUpdated()
                }
            }

            override fun onBillingServiceDisconnected() {
                ikLogger("Fail to connect with Google Play")
                isClientReady = false

                // callback with Main thread because billing throw it in IO thread
                CoroutineScope(Dispatchers.Main).launch {
                    premiumClientListener?.onIkClientInitError()
                }
            }
        })
    }

    private fun fetchIkAvailableAllSubsProducts(
        productListKeys: MutableList<String>,
        subsDeferred: CompletableDeferred<Unit>
    ) {
        // Early return if billing client is null
        val client = billingClient ?: run {
            ikLogger("Billing client null while fetching All Subscription Products")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            subsDeferred.complete(Unit)
            return
        }

        // Create a list of QueryProductDetailsParams.Product from the productListKeys
        val productList = productListKeys.map {
            ikLogger("Subscription key: $it")
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        // Build the QueryProductDetailsParams
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // Query product details asynchronously
        client.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                productDetailsList.forEach { productDetails ->
                    ikLogger("Subscription product details: $productDetails")
                    allProducts.add(productDetails)
                }
            } else {
                ikLogger("Failed to retrieve SUBS prices: ${billingResult.debugMessage}")
            }

            subsDeferred.complete(Unit)

        }
    }

    fun subscribeIkProduct(activity: Activity, productId: String, offerId: String = "") {
        if (billingClient != null) {
            val productInfo = getProductDetail(productId, offerId, BillingClient.ProductType.SUBS)
            if (productInfo != null) {
                val productDetailsParamsList = ArrayList<BillingFlowParams.ProductDetailsParams>()
                if (productInfo.productType == BillingClient.ProductType.SUBS && productInfo.subscriptionOfferDetails != null) {
                    val offerToken =
                        getIkOfferToken(productInfo.subscriptionOfferDetails, productId, offerId)
                    if (offerToken.trim { it <= ' ' } != "") {
                        productDetailsParamsList.add(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productInfo).setOfferToken(offerToken).build()
                        )
                    } else {
                        premiumEventListener?.onIkBillingError(IkBillingErrors.OFFER_NOT_EXIST)
                        ikLogger("The offer id: $productId doesn't seem to exist on Play Console")
                        return
                    }
                } else {
                    productDetailsParamsList.add(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productInfo).build()
                    )
                }
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList).build()
                billingClient!!.launchBillingFlow(activity, billingFlowParams)
            } else {
                premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
                ikLogger("Billing client can not launch billing flow because product details are missing")
            }
        } else {
            ikLogger("Billing client null while purchases")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
        }
    }

    private fun upgradeOrDowngradeSubscription(
        activity: Activity,
        updateProductId: String,
        updateOfferId: String,
        oldProductID: String,
        policy: Int
    ) {

        if (billingClient != null) {
            val productInfo =
                getProductDetail(updateProductId, updateOfferId, BillingClient.ProductType.SUBS)
            if (productInfo != null) {
                val oldToken = getIkOldPurchaseToken(oldProductID)
                if (oldToken.trim().isNotEmpty()) {
                    val productDetailsParamsList =
                        ArrayList<BillingFlowParams.ProductDetailsParams>()
                    if (productInfo.productType == BillingClient.ProductType.SUBS && productInfo.subscriptionOfferDetails != null) {
                        val offerToken = getIkOfferToken(
                            productInfo.subscriptionOfferDetails, updateProductId, updateOfferId
                        )
                        if (offerToken.trim { it <= ' ' } != "") {
                            productDetailsParamsList.add(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productInfo).setOfferToken(offerToken)
                                    .build()
                            )
                        } else {
                            premiumEventListener?.onIkBillingError(IkBillingErrors.OFFER_NOT_EXIST)
                            ikLogger("The offer id: $updateProductId doesn't seem to exist on Play Console")
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
                    ikLogger("old purchase token not found")
                    premiumEventListener?.onIkBillingError(IkBillingErrors.OLD_PURCHASE_TOKEN_NOT_FOUND)

                }
            } else {
                ikLogger("Billing client can not launch billing flow because product details are missing while update")
                premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
            }
        } else {
            ikLogger("Billing client null while Update subs")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
        }
    }

    private fun getIkOldPurchaseToken(basePlanKey: String): String {
        // Find the product that matches the subscription and base plan key
        val matchingProduct = allProducts.firstOrNull { product ->
            product.productType == BillingClient.ProductType.SUBS && product.subscriptionOfferDetails?.any { it.basePlanId == basePlanKey } == true
        }

        // If a matching product is found, find the corresponding purchase token
        matchingProduct?.let { product ->
            val matchingPurchase = purchasedSubsProductList.firstOrNull { purchase ->
                purchase.products.firstOrNull() == product.productId
            }
            return matchingPurchase?.purchaseToken ?: ""
        }

        // Return empty string if no matching product or purchase is found
        return ""
    }

    private fun getIkOfferToken(
        offerList: List<ProductDetails.SubscriptionOfferDetails>?,
        productId: String,
        offerId: String
    ): String {
        for (product in offerList!!) {
            if (product.offerId != null && product.offerId == offerId && product.basePlanId == productId) {
                return product.offerToken
            } else if (offerId.trim { it <= ' ' } == "" && product.basePlanId == productId && product.offerId == null) {
                // case when no offer in base plan
                return product.offerToken
            }
        }
        ikLogger("No Offer find")
        return ""
    }

    fun setSubKeys(keysList: MutableList<String>): IkBillingHelper {
        subKeys.addAll(keysList)
        return this
    }

    fun isSubsPremiumUser(): Boolean {
        return purchasedSubsProductList.isNotEmpty()
    }

    fun isSubsPremiumUserByBasePlanKey(basePlanKey: String): Boolean {
        val isPremiumUser = allProducts.any { product ->
            product.productType == BillingClient.ProductType.SUBS &&
                    product.subscriptionOfferDetails?.any { it.basePlanId == basePlanKey } == true &&
                    purchasedSubsProductList.any { it.products.firstOrNull() == product.productId }
        }

        if (!isPremiumUser) {
            premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        }

        return isPremiumUser
    }

    fun isSubsPremiumUserBySubIDKey(subId: String): Boolean {
        return purchasedSubsProductList.any { it.products.first() == subId }
    }

    fun areSubscriptionsSupported(): Boolean {
        return if (billingClient != null) {
            val responseCode =
                billingClient!!.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
            responseCode.responseCode == BillingClient.BillingResponseCode.OK
        } else {
            ikLogger("billing client null while check subscription support ")
            premiumEventListener?.onIkBillingError(IkBillingErrors.BILLING_UNAVAILABLE)

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
            ikLogger("Handling subscription cancellation: error while trying to unsubscribe")
            e.printStackTrace()
        }
    }

    //////////////////////////////////////////////////// In-App /////////////////////////////////////////////////////////////

    fun buyIkInApp(activity: Activity, productId: String, isPersonalizedOffer: Boolean = false) {
        val client = billingClient ?: run {
            ikLogger("Error: Billing client is null.")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            return
        }

        val productInfo = getProductDetail(productId, "", BillingClient.ProductType.INAPP)
        if (productInfo != null) {
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productInfo)
                    .build()
            )
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .setIsOfferPersonalized(isPersonalizedOffer)
                .build()

            client.launchBillingFlow(activity, billingFlowParams)
            ikLogger("Initiating purchase for IN-APP product: $productId")
        } else {
            ikLogger("Error: IN-APP product details missing for product ID: $productId")
            premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        }
    }


    private fun fetchIkAvailableAllInAppProducts(
        productListKeys: MutableList<String>,
        inAppDeferred: CompletableDeferred<Unit>
    ) {
        // Early return if billing client is null
        val client = billingClient ?: run {
            ikLogger("Billing client null while fetching All In-App Products")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            inAppDeferred.complete(Unit)
            return
        }

        val productList = productListKeys.map {
            ikLogger("In-App key: $it")
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
                    ikLogger("In-app product details: $productDetails")
                    allProducts.add(productDetails)
                }
            } else {
                ikLogger("Failed to retrieve In-APP prices: ${billingResult.debugMessage}")
            }
            inAppDeferred.complete(Unit)
        }
    }

    fun isInAppPremiumUser(): Boolean {
        return purchasedInAppProductList.isNotEmpty()
    }

    fun isInAppPremiumUserByInAppKey(inAppKey: String): Boolean {
        return purchasedInAppProductList.any { purchase ->
            purchase.products.any { it == inAppKey }
        }
    }

    fun setInAppKeys(keysList: MutableList<String>): IkBillingHelper {
        inAppKeys.addAll(keysList)
        return this
    }

    fun setConsumableKeys(keysList: MutableList<String>): IkBillingHelper {
        consumeAbleKeys.addAll(keysList)
        return this
    }

    ///////////////////////////////////////////////// Common ////////////////////////////////////////////////////////////

    fun getAllProductPrices(): MutableList<IkBillingProduct> {
        val priceList = mutableListOf<IkBillingProduct>()

        // Place try catch because billing internal class throw null pointer some time on ProductType
        try {
            allProducts.forEach {

                if (it.productType == BillingClient.ProductType.INAPP) {
                    val productPrice = IkBillingProduct()
                    productPrice.title = it.title
                    productPrice.type = it.productType
                    productPrice.subsKey = it.productId
                    productPrice.productBasePlanKey = ""
                    productPrice.productOfferKey = ""
                    productPrice.price = it.oneTimePurchaseOfferDetails?.formattedPrice.toString()
                    productPrice.duration = "lifeTime"
                    priceList.add(productPrice)
                } else {
                    it.subscriptionOfferDetails?.forEach { subIt ->
                        val productPrice = IkBillingProduct()
                        productPrice.title = it.title
                        productPrice.type = it.productType
                        productPrice.subsKey = it.productId
                        productPrice.productBasePlanKey = subIt.basePlanId
                        productPrice.productOfferKey = subIt.offerId.toString()
                        productPrice.price =
                            subIt.pricingPhases.pricingPhaseList.first().formattedPrice
                        productPrice.duration =
                            subIt.pricingPhases.pricingPhaseList.first().billingPeriod
                        priceList.add(productPrice)
                    }

                }
            }
        } catch (e: java.lang.Exception) {
            return mutableListOf()
        } catch (e: Exception) {
            return mutableListOf()
        }

        return priceList
    }

    fun getProductPriceByKey(basePlanKey: String, offerKey: String): IkBillingProduct? {
        // Place try catch because billing internal class throw null pointer some time on ProductType
        try {
            allProducts.forEach {
                if (it.productType == BillingClient.ProductType.SUBS) {
                    it.subscriptionOfferDetails?.forEach { subIt ->
                        if (offerKey.trim().isNotEmpty()) {
                            if (subIt.basePlanId == basePlanKey && subIt.offerId == offerKey) {
                                val productPrice = IkBillingProduct()
                                productPrice.title = it.title
                                productPrice.type = it.productType
                                productPrice.subsKey = it.productId
                                productPrice.productBasePlanKey = subIt.basePlanId
                                productPrice.productOfferKey = subIt.offerId.toString()
                                productPrice.price =
                                    subIt.pricingPhases.pricingPhaseList.first().formattedPrice
                                productPrice.duration =
                                    subIt.pricingPhases.pricingPhaseList.first().billingPeriod
                                return productPrice
                            }
                        } else {
                            if (subIt.basePlanId == basePlanKey && subIt.offerId == null) {
                                val productPrice = IkBillingProduct()
                                productPrice.title = it.title
                                productPrice.type = it.productType
                                productPrice.subsKey = it.productId
                                productPrice.productBasePlanKey = subIt.basePlanId
                                productPrice.productOfferKey = subIt.offerId.toString()
                                productPrice.price =
                                    subIt.pricingPhases.pricingPhaseList.first().formattedPrice
                                productPrice.duration =
                                    subIt.pricingPhases.pricingPhaseList.first().billingPeriod
                                return productPrice
                            }
                        }
                    }
                }

            }
        } catch (e: java.lang.Exception) {
            ///leave blank because below code auto handle this
        } catch (e: Exception) {
            ///leave blank because below code auto handle this
        }
        ikLogger("SUBS Product Price not found because product is missing")
        premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        return null
    }

    fun getProductPriceByKey(productKey: String): IkBillingProduct? {
        // Place try catch because billing internal class throw null pointer some time on ProductType
        try {
            allProducts.forEach {
                if (it.productType == BillingClient.ProductType.INAPP) {
                    if (it.productId == productKey) {
                        val productPrice = IkBillingProduct()
                        productPrice.title = it.title
                        productPrice.type = it.productType
                        productPrice.subsKey = it.productId
                        productPrice.productBasePlanKey = ""
                        productPrice.productOfferKey = ""
                        productPrice.price =
                            it.oneTimePurchaseOfferDetails?.formattedPrice.toString()
                        productPrice.duration = "lifeTime"
                        return productPrice
                    }
                }

            }
        } catch (e: java.lang.Exception) {
            ///leave blank because below code auto handle this
        } catch (e: Exception) {
            ///leave blank because below code auto handle this
        }
        ikLogger("IN-APP Product Price not found because product is missing")
        premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        return null
    }

    private fun handlePurchase(purchase: Purchase) {
        // Ensure billingClient is not null
        val billingClient = billingClient ?: run {
            ikLogger("Billing client is null while handling purchases")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            return
        }

        // Get the product type of the purchase
        val productType = getProductType(purchase.products.first())

        // Handle non-purchased states
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            ikLogger("No item purchased: ${purchase.packageName}")
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                ikLogger("Purchase is pending, cannot acknowledge until purchased")
                premiumEventListener?.onIkBillingError(IkBillingErrors.ACKNOWLEDGE_WARNING)
            }
            return
        }

        // Handle purchase acknowledgment
        if (!purchase.isAcknowledged) {
            acknowledgePurchase(billingClient, purchase, productType)
        } else {
            ikLogger("Item already acknowledged")
            purchasedSubsProductList.add(purchase)
            premiumClientListener?.onIkPurchasesUpdated()
        }

        // Handle consumable purchases
        if (consumeAbleKeys.contains(purchase.products.first())) {
            consumePurchase(billingClient, purchase)
        } else {
            ikLogger("This purchase is not consumable")
        }
    }

    // Helper function to acknowledge a purchase
    private fun acknowledgePurchase(
        billingClient: BillingClient,
        purchase: Purchase,
        productType: String
    ) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) {
            if (it.responseCode == BillingClient.BillingResponseCode.OK) {
                ikLogger("$productType item acknowledged")
                // Add purchase to the appropriate list
                if (productType.trim().isNotEmpty()) {
                    if (productType == BillingClient.ProductType.INAPP) {
                        purchasedInAppProductList.add(purchase)
                    } else {
                        purchasedSubsProductList.add(purchase)
                    }
                    premiumClientListener?.onIkPurchasesUpdated()
                } else {
                    ikLogger("Product type not found while handling purchase")
                }
                premiumEventListener?.onIkPurchaseAcknowledged(purchase)
            } else {
                ikLogger("Acknowledge error: ${it.debugMessage} (code: ${it.responseCode})")
                premiumEventListener?.onIkBillingError(IkBillingErrors.ACKNOWLEDGE_ERROR)
            }
        }
    }

    // Helper function to consume a purchase
    private fun consumePurchase(billingClient: BillingClient, purchase: Purchase) {
        val consumeParams =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                ikLogger("Purchase consumed")
                premiumEventListener?.onIkPurchaseConsumed(purchase)
            } else {
                ikLogger("Failed to consume purchase: ${result.debugMessage} (code: ${result.responseCode})")
                premiumEventListener?.onIkBillingError(IkBillingErrors.CONSUME_ERROR)
            }
        }
    }

    fun fetchActivePurchases(purchasesDeferred: CompletableDeferred<Unit> = CompletableDeferred()) {
        fetchAndUpdateActivePurchases(purchasesDeferred)
//        fetchActiveInAppPurchasesHistory()
    }

    private fun fetchAndUpdateActivePurchases(purchasesDeferred: CompletableDeferred<Unit>) {
        val billingClient = billingClient
        if (billingClient == null) {
            ikLogger("Billing client is null while fetching active purchases")
            premiumEventListener?.onIkBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            purchasesDeferred.complete(Unit)
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)

        fun handleBillingResult(
            billingResult: BillingResult,
            purchases: List<Purchase>,
            productType: String,
            purchasesDeferred: CompletableDeferred<Unit>
        ) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases =
                    purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                ikLogger("$productType purchases found: ${activePurchases.size}")

                if (activePurchases.isEmpty()) {
                    premiumClientListener?.onIkPurchasesUpdated()
                    purchasesDeferred.complete(Unit)
                    return
                }

                scope.launch {
                    activePurchases.forEach { purchase ->
                        ikLogger("$productType purchase: ${purchase.products.first()}")
                        handlePurchase(purchase)
                    }
                    purchasesDeferred.complete(Unit)
                }
            } else {
                ikLogger("No $productType purchases found")
            }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { billingResult, purchases ->
            handleBillingResult(billingResult, purchases, "SUBS", purchasesDeferred)
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            handleBillingResult(billingResult, purchases, "IN-APP", purchasesDeferred)
        }
    }

    fun getProductDetail(
        productKey: String,
        offerKey: String = "",
        productType: String
    ): ProductDetails? {

        val offerKeyNormalized = offerKey.trim().takeIf { it.isNotEmpty() } ?: "null"

        if (allProducts.isEmpty()) {
            premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
            return null
        }

        val product = allProducts.find { product ->
            when (productType) {
                BillingClient.ProductType.INAPP -> {
                    if (product.productId == productKey) {
                        ikLogger("In App product detail: title: ${product.title} price: ${product.oneTimePurchaseOfferDetails?.formattedPrice}")
                        true
                    } else {
                        false
                    }
                }

                BillingClient.ProductType.SUBS -> {
                    product.subscriptionOfferDetails?.any { subDetails ->
                        val isMatchingBasePlan = subDetails.basePlanId.equals(productKey, true)
                        val isMatchingOfferId =
                            subDetails.offerId.toString().equals(offerKeyNormalized, true)
                        if (isMatchingBasePlan && isMatchingOfferId) {
                            ikLogger("Subscription product detail: basePlanId: ${subDetails.basePlanId} offerId: ${subDetails.offerId}")
                        }
                        isMatchingBasePlan && isMatchingOfferId
                    } ?: false
                }

                else -> false
            }
        }

        if (product == null) {
            premiumEventListener?.onIkBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        }

        return product
    }

    private fun getProductType(productKey: String): String {
        allProducts.forEach { productDetail ->
            if (productDetail.productType == BillingClient.ProductType.INAPP) {
                if (productDetail.productId == productKey) {
                    return productDetail.productType
                }
            } else {
                productDetail.subscriptionOfferDetails?.forEach {
                    if (it.basePlanId == productKey) {
                        return productDetail.productType
                    }
                }
            }
        }
        return ""
    }

    fun isClientReady(): Boolean {
        return isClientReady
    }

    fun enableIkLogs(showLogs: Boolean = true): IkBillingHelper {
        enableLog = showLogs
        return this
    }

    private fun ikLogger(message: String) {
        if (enableLog) {
            Log.d(TAG, message)
        }
    }

    fun release() {
        if (billingClient != null && billingClient!!.isReady) {
            ikLogger("BillingHelper instance release: ending connection...")
            billingClient?.endConnection()
        }
    }

    fun setBillingEventListener(billingEventListeners: IkBillingEventListeners?): IkBillingHelper {
        premiumEventListener = billingEventListeners
        return this
    }

    fun setBillingClientListener(billingClientListeners: IkBillingClientListeners?): IkBillingHelper {
        premiumClientListener = billingClientListeners
        return this
    }
}
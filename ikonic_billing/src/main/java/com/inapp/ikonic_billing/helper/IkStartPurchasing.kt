package com.inapp.ikonic_billing.helper

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.inapp.ikonic_billing.billingDb.IkPurchasedProduct
import com.inapp.ikonic_billing.helper.IkBillingInfo.billingClient
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikClientListener
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikEventListener
import com.inapp.ikonic_billing.helper.IkBillingInfo.consumeAbleProductIds
import com.inapp.ikonic_billing.helper.IkBillingInfo.lastIkPurchasedProduct
import com.inapp.ikonic_billing.helper.IkBillingInfo.purchasedInAppProductList
import com.inapp.ikonic_billing.helper.IkBillingInfo.purchasedSubsProductList
import com.inapp.ikonic_billing.enum_class.IkBillingErrors

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkStartPurchasing {

    private val ikProductDetail = IkProductDetail()
    private val ikProductPrices = IkProductPrices()

    fun subscribeIkProduct(activity: Activity, basePlanId: String, offerId: String? = null) {
        billingClient?.let { client ->
            // Try to get product information with both basePlanId and offerId
            var productInfo = ikProductDetail.getIkProductDetail(basePlanId, offerId, BillingClient.ProductType.SUBS)
            var productPriceInfo = ikProductPrices.getIkSubscriptionProductPriceById(basePlanId = basePlanId, offerId = offerId)
            // Check if productInfo is null when using basePlanId and offerId
            var effectiveOfferId = offerId
            if (productInfo == null && effectiveOfferId != null) {
                ikEventListener?.onBillingError(IkBillingErrors.OFFER_NOT_EXIST)
                logger("The offer id: $offerId doesn't exist for basePlanId: $basePlanId on Play Console")
                // Retry with only basePlanId (set effectiveOfferId to null)
                effectiveOfferId = null

                productInfo = ikProductDetail.getIkProductDetail(basePlanId, null, BillingClient.ProductType.SUBS)
                productPriceInfo = ikProductPrices.getIkSubscriptionProductPriceById(basePlanId = basePlanId, offerId = null)
            }

            if (productInfo != null) {
                val productDetailsParamsList = mutableListOf<BillingFlowParams.ProductDetailsParams>()

                if (productInfo.productType == BillingClient.ProductType.SUBS && productInfo.subscriptionOfferDetails != null) {
                    val offerToken = ikProductDetail.getIkOfferToken(productInfo.subscriptionOfferDetails, basePlanId, effectiveOfferId)

                    if (offerToken.isNotBlank()) {
                        productDetailsParamsList.add(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productInfo)
                                .setOfferToken(offerToken)
                                .build()
                        )
                    } else {
                        ikEventListener?.onBillingError(IkBillingErrors.OFFER_NOT_EXIST)
                        logger("The offer id: $offerId doesn't seem to exist on Play Console")
                        return
                    }
                } else {
                    productDetailsParamsList.add(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productInfo)
                            .build()
                    )
                }
                // Initiate the billing flow
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                client.launchBillingFlow(activity, billingFlowParams)

                productPriceInfo?.let {
                    lastIkPurchasedProduct = IkPurchasedProduct(productId = productPriceInfo.productId,
                        basePlanId = productPriceInfo.basePlanId,
                        offerId = productPriceInfo.offerId,
                        title = productInfo.title,
                        type = productInfo.productType,
                        price = productPriceInfo.price,
                        priceMicro = productPriceInfo.priceMicro,
                        currencyCode = productPriceInfo.currencyCode)
                }

            } else {
                ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
                logger("Billing client cannot launch billing flow because product details for basePlanId: $basePlanId are missing")
            }
        } ?: run {
            // If billingClient is null, handle service disconnected error
            logger("Billing client is null while attempting purchase")
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
        }
    }

    fun buyIKInApp(activity: Activity, productId: String, isPersonalizedOffer: Boolean = false) {
        // Ensure billing client is available
        val client = billingClient ?: run {
            logger("Error: Billing client is null.")
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            return
        }
        // Get product details
        val productInfo = ikProductDetail.getIkProductDetail(productId = productId, offerId = null, productType = BillingClient.ProductType.INAPP)
        if (productInfo == null) {
            logger("Error: IN-APP product details missing for product ID: $productId")
            ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
            return
        }
        // Build the billing flow parameters and initiate the purchase
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productInfo)
                        .build()
                )
            )
            .setIsOfferPersonalized(isPersonalizedOffer)
            .build()
        // Launch the billing flow
        client.launchBillingFlow(activity, billingFlowParams)
        logger("Initiating purchase for IN-APP product: $productId")
    }

    fun handleIkPurchase(purchase: Purchase) {
        // Ensure billingClient is not null
        val billingClient = billingClient ?: run {
            logger("Billing client is null while handling purchases")
            ikEventListener?.onBillingError(IkBillingErrors.SERVICE_DISCONNECTED)
            return
        }
        // Get the product type of the purchase
        val productType = ikProductDetail.getIkProductType(purchase.products.first())
        // Handle non-purchased states
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            logger("No item purchased: ${purchase.packageName}")
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                logger("Purchase is pending, cannot acknowledge until purchased")
                ikEventListener?.onBillingError(IkBillingErrors.ACKNOWLEDGE_WARNING)
            }
            return
        }
        // Handle purchase acknowledgment
        if (!purchase.isAcknowledged) {
            acknowledgeIkPurchase(billingClient, purchase, productType)
        } else {
            logger("Item already acknowledged")
            purchasedSubsProductList.add(purchase)
            ikClientListener?.onPurchasesUpdated()
        }
        // Handle consumable purchases
        if (consumeAbleProductIds.contains(purchase.products.first())) {
            consumeIkPurchase(billingClient, purchase)
        } else {
            logger("This purchase is not consumable")
        }
    }

    private fun acknowledgeIkPurchase(billingClient: BillingClient, purchase: Purchase, productType: String) {
        val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgeParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                logger("$productType item acknowledged")
                when (productType) {
                    BillingClient.ProductType.INAPP -> purchasedInAppProductList.add(purchase)
                    BillingClient.ProductType.SUBS  -> purchasedSubsProductList.add(purchase)
                    else                            -> logger("Unknown product type while acknowledging purchase")
                }
                ikClientListener?.onPurchasesUpdated()
                ikEventListener?.onPurchaseAcknowledged(purchase)
            } else {
                logger("Acknowledge error: ${result.debugMessage} (code: ${result.responseCode})")
                ikEventListener?.onBillingError(IkBillingErrors.ACKNOWLEDGE_ERROR)
            }
        }
    }

    private fun consumeIkPurchase(billingClient: BillingClient, purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                logger("Purchase consumed")
                ikEventListener?.onPurchaseConsumed(purchase)
            } else {
                logger("Failed to consume purchase: ${result.debugMessage} (code: ${result.responseCode})")
                ikEventListener?.onBillingError(IkBillingErrors.CONSUME_ERROR)
            }
        }
    }
}
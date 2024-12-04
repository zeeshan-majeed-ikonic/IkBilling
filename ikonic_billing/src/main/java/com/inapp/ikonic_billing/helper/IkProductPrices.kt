package com.inapp.ikonic_billing.helper

import com.android.billingclient.api.BillingClient
import com.inapp.ikonic_billing.helper.IkBillingInfo.allIkProducts
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikEventListener
import com.inapp.ikonic_billing.data.IkPriceInfo
import com.inapp.ikonic_billing.enum_class.IkBillingErrors

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkProductPrices {

    fun getIkAllProductPrices(): MutableList<IkPriceInfo> {
        val priceList = mutableListOf<IkPriceInfo>()

        try {
            allIkProducts.forEach { ikProduct ->
                if (ikProduct.productType == BillingClient.ProductType.INAPP) {
                    // Handle in-app product pricing details
                    ikProduct.oneTimePurchaseOfferDetails?.let { offerDetails ->
                        priceList.add(IkPriceInfo().apply {
                            title = ikProduct.title
                            type = ikProduct.productType
                            productId = ikProduct.productId
                            basePlanId = ""
                            offerId = ""
                            price = offerDetails.formattedPrice
                            priceMicro = offerDetails.priceAmountMicros
                            currencyCode = offerDetails.priceCurrencyCode
                            duration = "lifeTime"
                        })
                    }
                } else {
                    // Handle subscription product pricing details
                    ikProduct.subscriptionOfferDetails?.forEach { subDetails ->
                        subDetails.pricingPhases.pricingPhaseList.firstOrNull()?.let { pricingPhase ->
                            priceList.add(IkPriceInfo().apply {
                                title = ikProduct.title
                                type = ikProduct.productType
                                productId = ikProduct.productId
                                basePlanId = subDetails.basePlanId
                                offerId = subDetails.offerId ?: ""
                                price = pricingPhase.formattedPrice
                                priceMicro = pricingPhase.priceAmountMicros
                                currencyCode = pricingPhase.priceCurrencyCode
                                duration = pricingPhase.billingPeriod
                            })
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return mutableListOf()
        }

        return priceList
    }

    fun getIkSubscriptionProductPriceById(basePlanId: String, offerId: String? = null): IkPriceInfo? {
        try {
            allIkProducts.forEach { ikProduct ->
                if (ikProduct.productType == BillingClient.ProductType.SUBS) {
                    ikProduct.subscriptionOfferDetails?.forEach { offerDetail ->
                        // Match based on basePlanId and offerId if it's not null
                        val isOfferMatch = offerId?.let { offerDetail.offerId == it } ?: (offerDetail.offerId == null)
                        if (offerDetail.basePlanId == basePlanId && isOfferMatch) {
                            return IkPriceInfo().apply {
                                title = ikProduct.title
                                type = ikProduct.productType
                                productId = ikProduct.productId
                                this.basePlanId = offerDetail.basePlanId
                                this.offerId = offerDetail.offerId.orEmpty()
                                price = offerDetail.pricingPhases.pricingPhaseList.first().formattedPrice
                                priceMicro = offerDetail.pricingPhases.pricingPhaseList.first().priceAmountMicros
                                currencyCode = offerDetail.pricingPhases.pricingPhaseList.first().priceCurrencyCode.toString()
                                duration = offerDetail.pricingPhases.pricingPhaseList.first().billingPeriod
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle any potential exceptions quietly, logging will occur outside of this block
        }

        logger("Failed to found Subscription Product price, basePlanId = $basePlanId, offerId = $offerId",true)
        ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        return null
    }

    fun getIkInAppProductPriceById(inAppProductId: String): IkPriceInfo? {
        try {
            allIkProducts.forEach { ikProduct ->
                if (ikProduct.productType == BillingClient.ProductType.INAPP && ikProduct.productId == inAppProductId) {
                    return IkPriceInfo().apply {
                        title = ikProduct.title
                        type = ikProduct.productType
                        productId = ikProduct.productId
                        basePlanId = ""
                        offerId = ""
                        ikProduct.oneTimePurchaseOfferDetails?.let { offerDetails ->
                            price = offerDetails.formattedPrice
                            priceMicro = offerDetails.priceAmountMicros
                            currencyCode = offerDetails.priceCurrencyCode
                        }
                        duration = "lifeTime"
                    }
                }
            }
        } catch (e: Exception) {
            // Log and handle any errors gracefully
        }

        logger("Unable to found IN-APP Product Price")
        ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        return null
    }
}
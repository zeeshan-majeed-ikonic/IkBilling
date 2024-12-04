package com.inapp.ikonic_billing.helper

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.inapp.ikonic_billing.helper.IkBillingInfo.allIkProducts
import com.inapp.ikonic_billing.helper.IkBillingInfo.ikEventListener
import com.inapp.ikonic_billing.enum_class.IkBillingErrors

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

class IkProductDetail {

    fun getIkProductDetail(productId: String, offerId: String? = null, productType: String): ProductDetails? {
        if (allIkProducts.isEmpty()) {
            ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
            return null
        }

        val product = allIkProducts.find { product ->
            when (productType) {
                BillingClient.ProductType.INAPP -> {
                    if (product.productId == productId) {
                        logger("In-App product detail: title=${product.title}, price=${product.oneTimePurchaseOfferDetails?.formattedPrice}")
                        true
                    } else {
                        false
                    }
                }

                BillingClient.ProductType.SUBS -> product.subscriptionOfferDetails?.any { subDetails ->
                    val isMatchingBasePlan = subDetails.basePlanId.equals(productId, true)
                    val isMatchingOfferId = (offerId == null || subDetails.offerId == offerId)

                    if (isMatchingBasePlan && isMatchingOfferId) {
                        logger("Subscription product detail: basePlanId = ${subDetails.basePlanId}, offerId = ${subDetails.offerId}")
                    }

                    isMatchingBasePlan && isMatchingOfferId
                } == true

                else -> false
            }
        }

        if (product == null) {
            ikEventListener?.onBillingError(IkBillingErrors.PRODUCT_NOT_EXIST)
        }

        return product
    }


    fun getIkOfferToken(offerList: List<ProductDetails.SubscriptionOfferDetails>?, basePlanId: String, offerId: String? = null): String {
        offerList?.forEach { product ->
            if (product.basePlanId == basePlanId && (offerId == null || product.offerId == offerId)) {
                return product.offerToken
            }

            if (offerId == null && product.basePlanId == basePlanId && product.offerId == null) {
                return product.offerToken
            }
        }

        logger("No offer found for basePlanId: $basePlanId and offerId: ${offerId ?: "null"}")
        return ""
    }

    fun getIkProductType(productKey: String): String {
        allIkProducts.forEach { productDetail ->
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
}
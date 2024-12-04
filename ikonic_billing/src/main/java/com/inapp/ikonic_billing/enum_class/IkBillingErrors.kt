package com.inapp.ikonic_billing.enum_class

import androidx.annotation.Keep

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

@Keep
enum class IkBillingErrors {
    DEVELOPER_ERROR,
    PRODUCT_NOT_EXIST,
    OFFER_NOT_EXIST,
    USER_CANCELED,
    SERVICE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    ITEM_UNAVAILABLE,
    ERROR,
    ITEM_ALREADY_OWNED,
    ITEM_NOT_OWNED,
    SERVICE_DISCONNECTED,
    ACKNOWLEDGE_ERROR,
    ACKNOWLEDGE_WARNING,
    OLD_PURCHASE_TOKEN_NOT_FOUND,
    CONSUME_ERROR
}

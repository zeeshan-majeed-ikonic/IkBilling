#### Ik Apps Billing

This is a simple and straight-forward implementation of Android 7.1.1 In-App-Billing API
> Support both In-App and Subscriptions.
>
### Getting Started 

## Step 01:

> Add Maven dependancies:

# Groovy-DSL

```kotlin 
    repositories {
          google()
          mavenCentral()
          maven { uri("https://jitpack.io") }
      }
```
# Kotlin-DSL

```kotlin 
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
```

## Step 02:

# Add gradle dependencies:

```kotlin 
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.zeeshan-majeed-ikonic:IkBilling:1.0.0")
```

## Step 03:

# Initilaize IkBillingClass

```kotlin 
    IkBillingHelper(context)
            .setInAppKeys(mutableListOf("your-inapp-id"))
            .setSubKeys(mutableListOf("your-product-id"))
            .initIkBilling()
```

 if consumable 

```kotlin 
    IkBillingHelper(context)
            .setInAppKeys(mutableListOf("your-inapp-id, inapp-consumable-key"))
            .setSubKeys(mutableListOf("your-product-id, inapp-consumable-key"))
            .initIkBilling()
```

## Step 04:

# Billing Client listeners

```kotlin 
    IkBillingHelper(context)
            .setInAppKeys(mutableListOf("your-inapp-id"))
            .setSubKeys(mutableListOf("your-product-id"))
            .setBillingClientListener(object : IkBillingClientListeners {
                override fun onIkPurchasesUpdated() {
                    Log.d(TAG, "onIkPurchasesUpdated: ")

                }

                override fun onIkClientReady() {
                    Log.d(TAG, "onIkClientReady: ")

                }

                override fun onIkClientInitError() {
                    Log.d(TAG, "onIkClientInitError: ")
                }

            })
            .initIkBilling()
```

## Step 05(optional):
# Enable logging

```kotlin 
    IkBillingHelper(context)
            .setInAppKeys(mutableListOf("your-inapp-id"))
            .setSubKeys(mutableListOf("your-product-id"))
            .enableIkLogs(showLogs = true)
            .initIkBilling()
```

## Step 06:
# Get prices

InApp price
```kotlin 
    fun getInAppPrice(context: Context): String {
        return IkBillingHelper(context).getProductPriceByKey("inapp-key")?.price ?: ""
    }
```

Subscription price

```kotlin 
    fun getSubsPrice(context: Context): String {
        return IkBillingHelper(context)
            .getProductPriceByKey("subscription-key", "")?.price ?: ""
    }
    fun getSubsPriceWithOffer(context: Context): String {
        return IkBillingHelper(context)
            .getProductPriceByKey("subscription-key", "offer-key")?.price ?: ""
    }
```

## Step 07:
# Buy Products

Buy InApp

```kotlin 
    fun buyInApp(activity: Activity){
        IkBillingHelper(activity).buyIkInApp(activity,"inapp-key",false)
    }
```

Subscribe subscription

> sub without offer

```kotlin 
    fun subscribe(activity: Activity){
        IkBillingHelper(activity).subscribeIkProduct(activity,"product-key","")
    }
```

> sub with offer

```kotlin 
    fun subscribeWithOffer(activity: Activity){
        IkBillingHelper(activity).subscribeIkProduct(activity,"product-key","offer-key")
    }
```

## Step 08:

# Handle Billing Listeners 

```kotlin 
    IkBillingHelper(this).setBillingEventListener(object : IkBillingEventListeners {
            override fun onIkProductsPurchased(purchases: List<Purchase?>) {

            }

            override fun onIkPurchaseAcknowledged(purchase: Purchase) {


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
```


# Ik Apps Billing

Simple and efficient library supporting both In-App Purchases and Subscriptions for Android. Using Google billing API version: 7.1.1

---

## **Get Started** 

### Step 01:

> Add Maven dependancies:

#### Groovy-DSL

```kotlin 
    repositories {
	mavenCentral()
	maven { url 'https://jitpack.io' }
     }
```
#### Kotlin-DSL

```kotlin 
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
```

### Step 02:

## Add gradle dependencies:
Version: [![](https://jitpack.io/v/zeeshan-majeed-ikonci/IkBilling.svg)](https://jitpack.io/#zeeshan-majeed-ikonci/IkBilling)

```kotlin 
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("com.github.zeeshan-majeed-ikonic:IkBilling:Version")
```

### Step 03:

## Initilaize IkBillingClass

```kotlin 
    IKBillingUtils(context)
            .setInAppProductKeys(mutableListOf("your-inapp-id"))
            .setSubProductKeys(mutableListOf("your-product-id"))
            .initIkBilling()
```

 if consumable 

```kotlin 
    IKBillingUtils(context)
            .setInAppProductKeys(mutableListOf("your-inapp-id, inapp-consumable-key"))
            .setSubProductKeys(mutableListOf("your-product-id, inapp-consumable-key"))
            .initIkBilling()
```

### Step 04:

## Billing Client listeners

```kotlin 
    IKBillingUtils(context)
            .setInAppProductKeys(mutableListOf("your-inapp-id"))
            .setSubProductKeys(mutableListOf("your-product-id"))
            .setIkClientListener(object : IkClientListener {
                override fun onPurchasesUpdated() {
                    Log.d(TAG, "onPurchasesUpdated: ")
                    CoroutineScope(Dispatchers.Main).launch {
                        premiumStatus = (ikBillingHelper.isHavingSubscription()
                                || ikBillingHelper.isHavingInApp()
                                || ikBillingHelper.isPremiumUser)
                    }

                }

                override fun onClientReady() {
                    Log.d(TAG, "onClientReady: ")
                    CoroutineScope(Dispatchers.Main).launch {
                        premiumStatus = (ikBillingHelper.isHavingSubscription()
                                || ikBillingHelper.isHavingInApp()
                                || ikBillingHelper.isPremiumUser)
                    }

                }

                override fun onClientInitError() {
                    Log.d(TAG, "onClientInitError: ")
                }

            })
            .initIkBilling()
```

### Step 05(optional):
## Enable logging

```kotlin 
    IKBillingUtils(context)
            .setInAppProductKeys(mutableListOf("your-inapp-id"))
            .setSubProductKeys(mutableListOf("your-product-id"))
            .enableLogger(showLogs = true)
            .initIkBilling()
```

### Step 06:
## Get prices

InApp price
```kotlin 
    fun getInAppPrice(context: Context): String {
        return IKBillingUtils(context).getIkInAppProductPriceById("inapp-key")?.price ?: ""
    }
```

Subscription price

```kotlin 
    fun getSubsPrice(context: Context): String {
        return IKBillingUtils(context)
            .getIkSubscriptionProductPriceById("subscription-key", "")?.price ?: ""
    }
    fun getSubsPriceWithOffer(context: Context): String {
        return IKBillingUtils(context)
            .getIkSubscriptionProductPriceById("subscription-key", "offer-key")?.price ?: ""
    }
```

### Step 07:
## Buy Products

Buy InApp

```kotlin 
    fun buyInApp(activity: Activity){
    IKBillingUtils(activity).buyIkInApp(activity,"inapp-key",false)
    }
```

Subscribe subscription

> sub without offer

```kotlin 
    fun subscribe(activity: Activity){
    IKBillingUtils(activity).subscribeIkProduct(activity,"product-key","")
    }
```

> sub with offer

```kotlin 
    fun subscribeWithOffer(activity: Activity){
    IKBillingUtils(activity).subscribeIkProduct(activity,"product-key","offer-key")
    }
```

### Step 08:

## Handle Billing Listeners 

```kotlin 
    IKBillingUtils(this).setIkEventListener(object : IkEventListener {
            override fun onProductsPurchased(purchases: List<Purchase?>) {
		        Log.d(TAG, "onIkProductsPurchased")
                CoroutineScope(Dispatchers.Main).launch {
                    premiumStatus = (ikBillingHelper.isHavingSubscription()
                            || ikBillingHelper.isHavingInApp()
                            || ikBillingHelper.isPremiumUser)
                }
            }

            override fun onPurchaseAcknowledged(purchase: Purchase) {
		        Log.d(TAG, "onIkPurchaseAcknowledged")
                CoroutineScope(Dispatchers.Main).launch {
                    premiumStatus = (ikBillingHelper.isHavingSubscription()
                            || ikBillingHelper.isHavingInApp()
                            || ikBillingHelper.isPremiumUser)
                }
            }

            override fun onPurchaseConsumed(purchase: Purchase) {
		        Log.d(TAG, "onIkPurchaseConsumed")
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
```


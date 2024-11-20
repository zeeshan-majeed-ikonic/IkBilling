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

### Step 04:

## Billing Client listeners

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

### Step 05(optional):
## Enable logging

```kotlin 
    IkBillingHelper(context)
            .setInAppKeys(mutableListOf("your-inapp-id"))
            .setSubKeys(mutableListOf("your-product-id"))
            .enableIkLogs(showLogs = true)
            .initIkBilling()
```

### Step 06:
## Get prices

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

### Step 07:
## Buy Products

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

### Step 08:

## Handle Billing Listeners 

```kotlin 
    IkBillingHelper(this).setBillingEventListener(object : IkBillingEventListeners {
            override fun onIkProductsPurchased(purchases: List<Purchase?>) {
		Log.d(TAG, "onIkProductsPurchased")
            }

            override fun onIkPurchaseAcknowledged(purchase: Purchase) {
		Log.d(TAG, "onIkPurchaseAcknowledged")
            }

            override fun onIkPurchaseConsumed(purchase: Purchase) {
		Log.d(TAG, "onIkPurchaseConsumed")
            }

            override fun onIkBillingError(error: IkBillingErrors) {
                when (error) {
                    	IkBillingErrors.DEVELOPER_ERROR -> Log.e(TAG, "Developer Error")
            		IkBillingErrors.CLIENT_NOT_READY -> Log.e(TAG, "Client Not Ready")
            		IkBillingErrors.CLIENT_DISCONNECTED -> Log.e(TAG, "Client Disconnected")
            		IkBillingErrors.PRODUCT_NOT_EXIST -> Log.e(TAG, "Product Does Not Exist")
            		IkBillingErrors.OFFER_NOT_EXIST -> Log.e(TAG, "Offer Does Not Exist")
            		IkBillingErrors.BILLING_ERROR -> Log.e(TAG, "General Billing Error")
            		IkBillingErrors.USER_CANCELED -> Log.e(TAG, "User Canceled")
            		IkBillingErrors.SERVICE_UNAVAILABLE -> Log.e(TAG, "Service Unavailable")
            		IkBillingErrors.BILLING_UNAVAILABLE -> Log.e(TAG, "Billing Unavailable")
            		IkBillingErrors.ITEM_UNAVAILABLE -> Log.e(TAG, "Item Unavailable")
            		IkBillingErrors.ITEM_ALREADY_OWNED -> Log.e(TAG, "Item Already Owned")
            		IkBillingErrors.ITEM_NOT_OWNED -> Log.e(TAG, "Item Not Owned")
            		IkBillingErrors.SERVICE_DISCONNECTED -> Log.e(TAG, "Service Disconnected")
            		IkBillingErrors.ACKNOWLEDGE_ERROR -> Log.e(TAG, "Acknowledgment Error")
            		IkBillingErrors.CONSUME_ERROR -> Log.e(TAG, "Consume Error")
            		else -> Log.e(TAG, "Unknown Billing Error")
		}
            }

        })
```


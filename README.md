#### Ik Apps Billing

This is a simple and straight-forward implementation of Android 7.1.1 In-App-Billing API
> Support both In-App and Subscriptions.
>
## Getting Started 

# Step 01:

> Add Maven dependancies:
> Groovy-DSL

```kotlin 
    repositories {
          google()
          mavenCentral()
          maven { uri("https://jitpack.io") }
      }
```
> Kotlin-DSL

```kotlin 
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
```

# Step 02:

> Add gradle dependencies:

```kotlin 
    implementation("com.android.billingclient:billing-ktx:7.1.1")
```

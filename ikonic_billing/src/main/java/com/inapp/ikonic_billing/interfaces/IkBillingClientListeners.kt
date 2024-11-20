package com.inapp.ikonic_billing.interfaces

/**
 * @author: Zeeshan
 * @date: 19/11/2024
 */

interface IkBillingClientListeners {
    fun onIkPurchasesUpdated()
    fun onIkClientReady()
    fun onIkClientAllReadyConnected(){}
    fun onIkClientInitError()
}
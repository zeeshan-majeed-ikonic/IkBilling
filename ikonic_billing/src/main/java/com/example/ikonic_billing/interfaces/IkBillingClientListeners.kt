package com.example.ikonic_billing.interfaces

interface IkBillingClientListeners {
    fun onIkPurchasesUpdated()
    fun onIkClientReady()
    fun onIkClientAllReadyConnected(){}
    fun onIkClientInitError()
}
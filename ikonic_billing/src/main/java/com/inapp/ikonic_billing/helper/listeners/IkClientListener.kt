package com.inapp.ikonic_billing.helper.listeners

interface IkClientListener {
    fun onPurchasesUpdated()
    fun onClientReady()
    fun onClientAllReadyConnected(){}
    fun onClientInitError()
}
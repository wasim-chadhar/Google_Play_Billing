package com.android.subscribe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.SkuDetailsParams
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var btn : Button;
    //Step 1
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn = findViewById(R.id.buyme)


        // Step 1
        var billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
//            getPrice()

        btn.setOnClickListener {
            //Step 2
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    // b. Show product available to buy
                    val productList = listOf(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("android.test.purchased")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )

                    // c. Querying with Kotlin extensions
                    val params = QueryProductDetailsParams.newBuilder()
                    params.setProductList(productList)

                    billingClient!!.queryProductDetailsAsync(params.build()){
                        billingResult, productDetailList ->
                        for (productDetails in productDetailList){
                            val offerToken = productDetails.subscriptionOfferDetails?.get(0)
                                ?.offerToken
                            val productDetailsParamsList = listOf(
                                offerToken?.let {
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .setOfferToken(it)
                                        .build()
                                }
                            )
                            val billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build()
                            val billingResult = billingClient!!.launchBillingFlow(this@MainActivity, billingFlowParams)
                        }
                    }

                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })


        }


    } //onCreate end
    //Step 1
    val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, Purchase ->

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && Purchase != null) {
                // The BillingClient is ready. You can query purchases here.
                for(purchase in Purchase){
                    handlePurchase(purchase)
                }

            }
            else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED){
                Toast.makeText(this , "ALREADY_SUBSCRIBED", Toast.LENGTH_LONG).show()

            }
            else if (billingResult.responseCode == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED){
                Toast.makeText(this , "FEATURE_NOT_SUPPORTED", Toast.LENGTH_LONG).show()

            }
            else{
                Toast.makeText(this, "ERROR"+ billingResult.debugMessage, Toast.LENGTH_SHORT).show()
            }
            // To be implemented in a later section.
        }

    private fun handlePurchase(purchase: Purchase?) {
        val consumeParams = purchase?.let {
            ConsumeParams.newBuilder()
                .setPurchaseToken(it.purchaseToken)
                .build()
        }
        val listener = ConsumeResponseListener { billingResult, s ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

            }
            }
            if (consumeParams != null) {
                billingClient!!.consumeAsync(consumeParams, listener)
            }
            if (purchase?.purchaseState == Purchase.PurchaseState.PURCHASED) {

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient!!.acknowledgePurchase(
                        acknowledgePurchaseParams,
                        acknowledgePurchaseResponseListener
                    )
                    Toast.makeText(this, "SUBSCRIBED SUCCESSFULLY", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "ALREADY SUBSCRIBED", Toast.LENGTH_SHORT).show()
                }

            } else if (purchase?.purchaseState == Purchase.PurchaseState.PENDING) {
                Toast.makeText(this, "SUBSCRIPTION PENDING", Toast.LENGTH_SHORT).show()
            } else if (purchase?.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                Toast.makeText(this, "UNSATISFIED STATE", Toast.LENGTH_SHORT).show()

            }

    }
    var acknowledgePurchaseResponseListener =
        AcknowledgePurchaseResponseListener{
        billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this, "SUCCESSFULLY SUBSCRIBED", Toast.LENGTH_SHORT).show()
            }
            }

    private fun getPrice(){
        billingClient!!.startConnection(object : BillingClientStateListener{
            override fun onBillingServiceDisconnected() {
//                TODO("Not yet implemented")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val executorService = Executors.newSingleThreadExecutor()
                executorService.execute{
                    val productList =
                        listOf(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId("android.test.purchased")
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()

                        )
                    val params = QueryProductDetailsParams.newBuilder()
                    params.setProductList(productList)

                    billingClient!!.queryProductDetailsAsync(params.build()) { billingResult, productDetailList ->
                        for (productDetail in productDetailList){
                            var responce =
                                productDetail.subscriptionOfferDetails?.get(0)?.pricingPhases
                                    ?.pricingPhaseList?.get(0)?.formattedPrice
                            val sku = productDetail.name
                            val ds = productDetail.description
                            var des = "$sku : $ds : price : $responce"
                        }
                    }
                }

                // runOnUiThread{}
            }

        })
    }

} // main end

/*
a. Initialize a connection to Google Play
        Add the Google Play Billing Library dependency
        1. Initialize a BillingClient
        2. Connect to Google Play

b. Show products available to buy
c. Querying with Kotlin extensions
 */
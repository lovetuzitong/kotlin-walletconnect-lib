package io.walletconnect.example

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.screen_main.*
import android.content.Intent
import android.net.Uri
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.walletconnect.Session
import org.walletconnect.nullOnThrow


class MainActivity : Activity(), Session.Callback {

    private var txRequest: Long? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onStatus(status: Session.Status) {
        when(status) {
            Session.Status.Approved -> sessionApproved()
            Session.Status.Closed -> sessionClosed()
            Session.Status.Connected,
            Session.Status.Disconnected,
            is Session.Status.Error -> {
                // Do Stuff
            }
        }
    }

    override fun onMethodCall(call: Session.MethodCall) {
    }
    private fun sessionApproved() {
        uiScope.launch {
            screen_main_status.text = "Connected Network: ${ExampleApplication.session.chainId()}\n Accounts: ${ExampleApplication.session.approvedAccounts()}"
            screen_main_connect_button.visibility = View.GONE
            screen_main_disconnect_button.visibility = View.VISIBLE
            screen_main_tx_button.visibility = View.VISIBLE
            screen_main_add_network.visibility = View.VISIBLE
            screen_main_watch_asserts.visibility = View.VISIBLE
            screen_main_gas_price.visibility = View.VISIBLE
        }
    }

    private fun sessionClosed() {
        uiScope.launch {
            screen_main_status.text = "Disconnected"
            screen_main_connect_button.visibility = View.VISIBLE
            screen_main_disconnect_button.visibility = View.GONE
            screen_main_tx_button.visibility = View.GONE
            screen_main_add_network.visibility = View.GONE
            screen_main_watch_asserts.visibility = View.GONE
            screen_main_gas_price.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_main)
    }

    private fun openWallet(){
        val i = Intent(Intent.ACTION_VIEW)
        i.data = Uri.parse(ExampleApplication.config.toWCUri())
        startActivity(i)
    }

    override fun onStart() {
        super.onStart()
        initialSetup()
        screen_main_connect_button.setOnClickListener {
            ExampleApplication.resetSession()
            ExampleApplication.session.addCallback(this)
            openWallet()
        }
        screen_main_disconnect_button.setOnClickListener {
            ExampleApplication.session.kill()
        }
        screen_main_tx_button.setOnClickListener {
            val from = ExampleApplication.session.approvedAccounts()?.first()
                    ?: return@setOnClickListener
            val txRequest = System.currentTimeMillis()
            ExampleApplication.session.performMethodCall(
                    Session.MethodCall.SendTransaction(
                            txRequest,
                            from,
                            "0xc7e5463a2646A7611c961537400448BE54ae1733",
                            null,
                            null,
                            null,
                            "0x9184e72a",
                            "0x"
                    ),
                    ::handleResponse
            )
            this.txRequest = txRequest
            openWallet()
        }
        screen_main_add_network.setOnClickListener {
            ExampleApplication.session.performMethodCall(
                Session.MethodCall.Custom(
                    10001,
                    "wallet_addEthereumChain",
                    listOf(mapOf(
                        "chainId" to "0x13881",
                        "chainName" to "Polygon Mumbai Testnet",
                        "rpcUrls" to listOf("https://matic-mumbai.chainstacklabs.com"),
                        "nativeCurrency" to mapOf(
                            "name" to "Polygon MATIC",
                            "symbol" to "MATIC",
                            "decimals" to 18
                        ),
                        "blockExplorerUrls" to listOf("https://mumbai.polygonscan.com/")
                    ))
                ),
                ::handleResponse
            )
            openWallet()
        }

        screen_main_watch_asserts.setOnClickListener {
            val params = mapOf(
                "type" to "ERC20",
                "options" to mapOf(
                    "address" to "0x1565544dA78F37D166334D697B966b1eC04428e4",
                    "symbol" to "TsT",
                    "decimals" to 18
                )
            )
            ExampleApplication.session.performMethodCall(
                Session.MethodCall.Custom(
                    10002,
                    "wallet_watchAsset",
                    params
                ),
                ::handleResponse
            )
            openWallet()
        }

        screen_main_gas_price.setOnClickListener {
            ExampleApplication.session.performMethodCall(
                Session.MethodCall.Custom(
                    10003,
                    "eth_gasPrice"
                ),
                ::handleResponse
            )
        }
    }

    private fun initialSetup() {
        val session = nullOnThrow { ExampleApplication.session } ?: return
        session.addCallback(this)
        sessionApproved()
    }

    private fun handleResponse(resp: Session.MethodCall.Response) {
        if (resp.id == txRequest) {
            txRequest = null
            uiScope.launch {
                screen_main_response.visibility = View.VISIBLE
                screen_main_response.text = "Last response: " + ((resp.result as? String) ?: "Unknown response")
            }
        }else if(resp.id == 10001.toLong()){
            uiScope.launch {
                screen_main_response.visibility = View.VISIBLE
                screen_main_response.text = "Last response: " + ((resp.result as? String) ?: "Unknown response")
            }
        }else if(resp.id == 10003.toLong()){
            uiScope.launch {
                screen_main_response.visibility = View.VISIBLE
                screen_main_response.text = "Gas Price = ${resp.result}"
            }
        }
    }

    override fun onDestroy() {
        ExampleApplication.session.removeCallback(this)
        super.onDestroy()
    }
}

package io.walletconnect.example

import androidx.multidex.MultiDexApplication
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.komputing.khex.extensions.toNoPrefixHexString
import org.walletconnect.Session
import org.walletconnect.impls.*
import org.walletconnect.nullOnThrow
import java.io.File
import java.util.*

class ExampleApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        initMoshi()
        initClient()
        initSessionStorage()
    }

    private fun initClient() {
        client = OkHttpClient.Builder().build()
    }

    private fun initMoshi() {
        moshi = Moshi.Builder().build()
    }

    private fun initSessionStorage() {
        storage = FileWCSessionStore(File(cacheDir, "session_store.json").apply { createNewFile() }, moshi)
    }

    companion object {
        private lateinit var client: OkHttpClient
        private lateinit var moshi: Moshi
        private lateinit var storage: WCSessionStore
        lateinit var config: Session.Config
        lateinit var session: Session

        fun resetSession() {
            nullOnThrow { session }?.clearCallbacks()
            val key = ByteArray(32).also { Random().nextBytes(it) }.toNoPrefixHexString()
            config = Session.Config(
                UUID.randomUUID().toString(),
                "https://bridge.walletconnect.org",
                key
            )
            session = WCSession(
                config.toFullyQualifiedConfig(),
                MoshiPayloadAdapter(moshi),
                storage,
                OkHttpTransport.Builder(client, moshi),
                Session.PeerMeta(
                    url="https://www.walletconnect.com/",
                    name = "Demo App",
                    description = "WalletConnect Test from Android"
                )
            )
            session.offer()
        }
    }
}

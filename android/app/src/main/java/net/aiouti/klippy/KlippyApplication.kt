package net.aiouti.klippy

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class KlippyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android ships with a limited BouncyCastle provider - replace it with the full one
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}

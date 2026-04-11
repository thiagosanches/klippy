package net.aiouti.klippy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var keyRepository: KeyRepository
    private lateinit var apiClient: ApiClient
    private lateinit var cryptoHelper: CryptoHelper
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var textView: MaterialTextView
    private lateinit var fabPush: FloatingActionButton
    private lateinit var fabPull: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        keyRepository = KeyRepository(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        textView = findViewById(R.id.textView)
        fabPush = findViewById(R.id.fabPush)
        fabPull = findViewById(R.id.fabPull)

        val serverUrl = keyRepository.getServerUrl()
        apiClient = ApiClient(serverUrl ?: "")
        
        val privateKey = keyRepository.getPrivateKey()
        val publicKey = keyRepository.getPublicKey()
        if (privateKey != null && publicKey != null) {
            cryptoHelper = CryptoHelper(privateKey, publicKey)
        }

        fabPush.setOnClickListener { pushClipboard() }
        fabPull.setOnClickListener { pullClipboard() }

        checkInitialSetup()
        
        // Handle share intent from other apps
        handleSharedText(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleSharedText(it) }
    }
    
    private fun handleSharedText(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                // Check if setup is complete
                if (!::cryptoHelper.isInitialized) {
                    Toast.makeText(this, R.string.please_configure_settings, Toast.LENGTH_LONG).show()
                    return
                }
                
                // Show what was shared
                Toast.makeText(this, getString(R.string.sharing_text), Toast.LENGTH_SHORT).show()
                
                // Push the shared text directly
                pushText(sharedText)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkInitialSetup() {
        if (keyRepository.getServerUrl() == null || 
            keyRepository.getPrivateKey() == null || 
            keyRepository.getPublicKey() == null) {
            textView.text = getString(R.string.setup_required)
            fabPush.isEnabled = false
            fabPull.isEnabled = false
            Toast.makeText(this, R.string.please_configure_settings, Toast.LENGTH_LONG).show()
        } else {
            textView.text = getString(R.string.ready)
            fabPush.isEnabled = true
            fabPull.isEnabled = true
        }
    }

    private fun pushClipboard() {
        if (!::cryptoHelper.isInitialized) {
            Toast.makeText(this, R.string.crypto_not_initialized, Toast.LENGTH_SHORT).show()
            return
        }

        val clipData = clipboardManager.primaryClip
        if (clipData == null || clipData.itemCount == 0) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val text = clipData.getItemAt(0).text?.toString()
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
            return
        }

        pushText(text)
    }
    
    private fun pushText(text: String) {
        if (!::cryptoHelper.isInitialized) {
            Toast.makeText(this, R.string.crypto_not_initialized, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                textView.text = getString(R.string.encrypting)
                val encrypted = withContext(Dispatchers.Default) {
                    cryptoHelper.encrypt(text)
                }
                
                textView.text = getString(R.string.sending)
                val success = withContext(Dispatchers.IO) {
                    apiClient.pushClipboard(encrypted)
                }
                
                if (success) {
                    textView.text = getString(R.string.push_success)
                    Toast.makeText(this@MainActivity, R.string.push_success, Toast.LENGTH_SHORT).show()
                } else {
                    textView.text = getString(R.string.push_failed)
                    Toast.makeText(this@MainActivity, R.string.push_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                textView.text = getString(R.string.error_occurred)
                Toast.makeText(this@MainActivity, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pullClipboard() {
        if (!::cryptoHelper.isInitialized) {
            Toast.makeText(this, R.string.crypto_not_initialized, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                textView.text = getString(R.string.fetching)
                val encrypted = withContext(Dispatchers.IO) {
                    apiClient.pullClipboard()
                }
                
                if (encrypted == null) {
                    textView.text = getString(R.string.no_data)
                    Toast.makeText(this@MainActivity, R.string.no_data_on_server, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                textView.text = getString(R.string.decrypting)
                val decrypted = withContext(Dispatchers.Default) {
                    cryptoHelper.decrypt(encrypted)
                }
                
                val clip = ClipData.newPlainText("klippy", decrypted)
                clipboardManager.setPrimaryClip(clip)
                
                textView.text = getString(R.string.pull_success)
                Toast.makeText(this@MainActivity, R.string.pull_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                textView.text = getString(R.string.error_occurred)
                Toast.makeText(this@MainActivity, "${getString(R.string.error)}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val serverUrl = keyRepository.getServerUrl()
        if (serverUrl != null) {
            apiClient = ApiClient(serverUrl)
        }
        
        val privateKey = keyRepository.getPrivateKey()
        val publicKey = keyRepository.getPublicKey()
        if (privateKey != null && publicKey != null) {
            cryptoHelper = CryptoHelper(privateKey, publicKey)
        }
        
        checkInitialSetup()
    }
}

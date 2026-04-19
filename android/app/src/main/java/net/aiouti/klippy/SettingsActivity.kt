package net.aiouti.klippy

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class SettingsActivity : AppCompatActivity() {
    private lateinit var keyRepository: KeyRepository
    private lateinit var serverUrlInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var privateKeyStatus: MaterialTextView
    private lateinit var publicKeyStatus: MaterialTextView
    private lateinit var saveButton: MaterialButton
    private lateinit var generateKeysButton: MaterialButton
    private lateinit var loadPrivateKeyButton: MaterialButton
    private lateinit var loadPublicKeyButton: MaterialButton
    
    private var privateKeyContent: String? = null
    private var publicKeyContent: String? = null

    private val privateKeyPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { loadKeyFile(it, isPrivate = true) }
    }

    private val publicKeyPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { loadKeyFile(it, isPrivate = false) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        keyRepository = KeyRepository(this)
        
        serverUrlInput = findViewById(R.id.serverUrlInput)
        privateKeyStatus = findViewById(R.id.privateKeyStatus)
        publicKeyStatus = findViewById(R.id.publicKeyStatus)
        saveButton = findViewById(R.id.saveButton)
        generateKeysButton = findViewById(R.id.generateKeysButton)
        loadPrivateKeyButton = findViewById(R.id.loadPrivateKeyButton)
        loadPublicKeyButton = findViewById(R.id.loadPublicKeyButton)

        loadSettings()

        saveButton.setOnClickListener { saveSettings() }
        generateKeysButton.setOnClickListener { generateKeys() }
        loadPrivateKeyButton.setOnClickListener { 
            privateKeyPicker.launch(arrayOf("*/*"))
        }
        loadPublicKeyButton.setOnClickListener {
            publicKeyPicker.launch(arrayOf("*/*"))
        }
    }

    private fun loadSettings() {
        serverUrlInput.setText(keyRepository.getServerUrl() ?: "")
        
        privateKeyContent = keyRepository.getPrivateKey()
        publicKeyContent = keyRepository.getPublicKey()
        
        updateKeyStatus()
    }

    private fun updateKeyStatus() {
        privateKeyStatus.text = if (privateKeyContent.isNullOrEmpty()) {
            getString(R.string.no_key_loaded)
        } else {
            getString(R.string.key_loaded)
        }
        
        publicKeyStatus.text = if (publicKeyContent.isNullOrEmpty()) {
            getString(R.string.no_key_loaded)
        } else {
            getString(R.string.key_loaded)
        }
    }

    private fun loadKeyFile(uri: Uri, isPrivate: Boolean) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IllegalStateException("Could not open file")
            
            if (isPrivate) {
                privateKeyContent = content
                Toast.makeText(this, R.string.private_key_loaded, Toast.LENGTH_SHORT).show()
            } else {
                publicKeyContent = content
                Toast.makeText(this, R.string.public_key_loaded, Toast.LENGTH_SHORT).show()
            }
            
            updateKeyStatus()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "${getString(R.string.error_loading_file)}: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun saveSettings() {
        val serverUrl = serverUrlInput.text?.toString()?.trim()

        if (serverUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.server_url_required, Toast.LENGTH_SHORT).show()
            return
        }

        // Validate URL format
        try {
            val url = URL(serverUrl)
            if (url.protocol != "http" && url.protocol != "https") {
                Toast.makeText(this, R.string.invalid_server_url, Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.invalid_server_url, Toast.LENGTH_SHORT).show()
            return
        }

        if (privateKeyContent.isNullOrEmpty() || publicKeyContent.isNullOrEmpty()) {
            Toast.makeText(this, R.string.keys_required, Toast.LENGTH_SHORT).show()
            return
        }

        keyRepository.saveServerUrl(serverUrl)
        keyRepository.savePrivateKey(privateKeyContent!!)
        keyRepository.savePublicKey(publicKeyContent!!)

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generateKeys() {
        generateKeysButton.isEnabled = false
        generateKeysButton.text = getString(R.string.generating)

        lifecycleScope.launch {
            try {
                val keyPair = withContext(Dispatchers.Default) {
                    CryptoHelper.generateKeyPair()
                }
                
                privateKeyContent = keyPair.first
                publicKeyContent = keyPair.second
                
                updateKeyStatus()
                
                Toast.makeText(this@SettingsActivity, R.string.keys_generated, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@SettingsActivity,
                    "${getString(R.string.error)}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                generateKeysButton.isEnabled = true
                generateKeysButton.text = getString(R.string.generate_keys)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

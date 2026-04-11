package net.aiouti.klippy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    private lateinit var keyRepository: KeyRepository
    private lateinit var serverUrlInput: TextInputEditText
    private lateinit var privateKeyInput: TextInputEditText
    private lateinit var publicKeyInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var generateKeysButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        keyRepository = KeyRepository(this)
        
        serverUrlInput = findViewById(R.id.serverUrlInput)
        privateKeyInput = findViewById(R.id.privateKeyInput)
        publicKeyInput = findViewById(R.id.publicKeyInput)
        saveButton = findViewById(R.id.saveButton)
        generateKeysButton = findViewById(R.id.generateKeysButton)

        loadSettings()

        saveButton.setOnClickListener { saveSettings() }
        generateKeysButton.setOnClickListener { generateKeys() }
    }

    private fun loadSettings() {
        serverUrlInput.setText(keyRepository.getServerUrl() ?: "")
        privateKeyInput.setText(keyRepository.getPrivateKey() ?: "")
        publicKeyInput.setText(keyRepository.getPublicKey() ?: "")
    }

    private fun saveSettings() {
        val serverUrl = serverUrlInput.text?.toString()?.trim()
        val privateKey = privateKeyInput.text?.toString()?.trim()
        val publicKey = publicKeyInput.text?.toString()?.trim()

        if (serverUrl.isNullOrEmpty()) {
            Toast.makeText(this, R.string.server_url_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (privateKey.isNullOrEmpty() || publicKey.isNullOrEmpty()) {
            Toast.makeText(this, R.string.keys_required, Toast.LENGTH_SHORT).show()
            return
        }

        keyRepository.saveServerUrl(serverUrl)
        keyRepository.savePrivateKey(privateKey)
        keyRepository.savePublicKey(publicKey)

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generateKeys() {
        generateKeysButton.isEnabled = false
        generateKeysButton.text = getString(R.string.generating)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val keyPair = withContext(Dispatchers.Default) {
                    CryptoHelper.generateKeyPair()
                }
                
                privateKeyInput.setText(keyPair.first)
                publicKeyInput.setText(keyPair.second)
                
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

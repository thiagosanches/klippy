package net.aiouti.klippy

import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.openpgp.*
import org.bouncycastle.openpgp.operator.jcajce.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date

class CryptoHelper(
    private val privateKeyArmored: String,
    private val publicKeyArmored: String
) {

    fun encrypt(plainText: String): String {
        try {
            val publicKey = readPublicKey(publicKeyArmored)
            val encryptor = PGPEncryptedDataGenerator(
                JcePGPDataEncryptorBuilder(PGPEncryptedData.AES_256)
                    .setWithIntegrityPacket(true)
                    .setSecureRandom(java.security.SecureRandom())
                    .setProvider("BC")
            )

            encryptor.addMethod(JcePublicKeyKeyEncryptionMethodGenerator(publicKey).setProvider("BC"))

            val outStream = ByteArrayOutputStream()
            val armoredOut = ArmoredOutputStream(outStream)
            val encryptedOut = encryptor.open(armoredOut, ByteArray(4096))

            val literalData = PGPLiteralDataGenerator()
            val literalOut = literalData.open(
                encryptedOut,
                PGPLiteralData.BINARY,
                "",
                plainText.toByteArray().size.toLong(),
                Date()
            )

            literalOut.write(plainText.toByteArray())
            literalOut.close()
            encryptedOut.close()
            armoredOut.close()

            return outStream.toString("UTF-8")
        } catch (e: Exception) {
            throw IllegalStateException("Encryption failed: ${e.message}", e)
        }
    }

    fun decrypt(encryptedArmored: String): String {
        val privateKey = readPrivateKey(privateKeyArmored)
        val inputStream = PGPUtil.getDecoderStream(ByteArrayInputStream(encryptedArmored.toByteArray()))
        val pgpObjectFactory = PGPObjectFactory(inputStream, JcaKeyFingerprintCalculator())

        val encDataList = pgpObjectFactory.nextObject() as PGPEncryptedDataList
        val encData = encDataList.iterator().next() as PGPPublicKeyEncryptedData

        val decryptor = JcePublicKeyDataDecryptorFactoryBuilder()
            .setProvider("BC")
            .build(privateKey)

        val clearStream = encData.getDataStream(decryptor)
        val plainFactory = PGPObjectFactory(clearStream, JcaKeyFingerprintCalculator())
        val literalData = plainFactory.nextObject() as PGPLiteralData

        val decryptedStream = literalData.inputStream
        val decryptedBytes = decryptedStream.readBytes()

        return String(decryptedBytes)
    }

    private fun readPublicKey(armoredKey: String): PGPPublicKey {
        try {
            val inputStream = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredKey.toByteArray()))
            val keyRingCollection = PGPPublicKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())
            
            // First try to find a subkey marked for encryption
            keyRingCollection.keyRings.forEach { keyRing ->
                keyRing.publicKeys.forEach { key ->
                    if (key.isEncryptionKey && !key.isMasterKey) {
                        return key
                    }
                }
            }
            
            // If no encryption subkey found, use the master key if it supports encryption
            keyRingCollection.keyRings.forEach { keyRing ->
                keyRing.publicKeys.forEach { key ->
                    if (key.isEncryptionKey) {
                        return key
                    }
                }
            }
            
            // Last resort: use the first master key (for self-generated keys)
            keyRingCollection.keyRings.forEach { keyRing ->
                val masterKey = keyRing.publicKey
                if (masterKey != null) {
                    return masterKey
                }
            }
            
            throw IllegalArgumentException("No encryption key found in public key")
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalArgumentException("Failed to parse public key: ${e.javaClass.simpleName} - ${e.message}", e)
        }
    }

    private fun readPrivateKey(armoredKey: String): PGPPrivateKey {
        val inputStream = PGPUtil.getDecoderStream(ByteArrayInputStream(armoredKey.toByteArray()))
        val keyRingCollection = PGPSecretKeyRingCollection(inputStream, JcaKeyFingerprintCalculator())
        
        val extractor = JcePBESecretKeyDecryptorBuilder()
            .setProvider("BC")
            .build("".toCharArray())
        
        // First try to find an encryption subkey
        keyRingCollection.keyRings.forEach { keyRing ->
            keyRing.secretKeys.forEach { secretKey ->
                if (secretKey.isSigningKey && !secretKey.isMasterKey) {
                    try {
                        return secretKey.extractPrivateKey(extractor)
                    } catch (e: Exception) {
                        // Continue to next key
                    }
                }
            }
        }
        
        // Try the master key
        keyRingCollection.keyRings.forEach { keyRing ->
            keyRing.secretKeys.forEach { secretKey ->
                if (secretKey.isMasterKey) {
                    try {
                        return secretKey.extractPrivateKey(extractor)
                    } catch (e: Exception) {
                        // Continue to next key
                    }
                }
            }
        }
        
        // Last resort: try any signing key
        keyRingCollection.keyRings.forEach { keyRing ->
            keyRing.secretKeys.forEach { secretKey ->
                if (secretKey.isSigningKey) {
                    try {
                        return secretKey.extractPrivateKey(extractor)
                    } catch (e: Exception) {
                        // Continue to next key
                    }
                }
            }
        }
        
        throw IllegalArgumentException("No private key found or failed to extract")
    }

    companion object {
        fun generateKeyPair(): Pair<String, String> {
            val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA", "BC")
            keyPairGenerator.initialize(4096)
            val keyPair = keyPairGenerator.generateKeyPair()

            val pgpKeyPair = JcaPGPKeyPair(
                PGPPublicKey.RSA_GENERAL,
                keyPair,
                Date()
            )

            val identity = "Klippy <klippy@aiouti.net>"
            val sha256Calc = JcaPGPDigestCalculatorProviderBuilder()
                .setProvider("BC")
                .build()
                .get(HashAlgorithmTags.SHA256)

            val keyRingGenerator = PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                pgpKeyPair,
                identity,
                sha256Calc,
                null,
                null,
                JcaPGPContentSignerBuilder(pgpKeyPair.publicKey.algorithm, HashAlgorithmTags.SHA256),
                JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc)
                    .setProvider("BC")
                    .build("".toCharArray())
            )

            val publicKeyRing = keyRingGenerator.generatePublicKeyRing()
            val secretKeyRing = keyRingGenerator.generateSecretKeyRing()

            val publicOut = ByteArrayOutputStream()
            val publicArmored = ArmoredOutputStream(publicOut)
            publicKeyRing.encode(publicArmored)
            publicArmored.close()

            val privateOut = ByteArrayOutputStream()
            val privateArmored = ArmoredOutputStream(privateOut)
            secretKeyRing.encode(privateArmored)
            privateArmored.close()

            return Pair(privateOut.toString("UTF-8"), publicOut.toString("UTF-8"))
        }
    }
}

package com.cloudnest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * AES-256-GCM file encryption.
 *
 * The configured secret can be ANY length — it is SHA-256 hashed to derive
 * a deterministic 32-byte (256-bit) AES key, then used directly. This means
 * the yml value never has to be exactly 32 characters.
 *
 * File format: [12-byte IV][ciphertext+GCM tag]
 */
@Slf4j
@Service
public class EncryptionService {

    private static final int IV_LENGTH = 12;          // 96-bit IV for GCM
    private static final int TAG_LENGTH_BITS = 128;   // 128-bit auth tag
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.secret-key}") String configuredKey) {
        this.secretKey = deriveKey(configuredKey);
        log.info("EncryptionService initialized with derived AES-256 key");
    }

    /** Hash the configured string to a deterministic 32-byte AES key. */
    private static SecretKeySpec deriveKey(String raw) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(raw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES key", e);
        }
    }

    public File encryptFile(File input) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plaintext = Files.readAllBytes(input.toPath());
            byte[] ciphertext = cipher.doFinal(plaintext);

            File output = File.createTempFile("cloudnest-enc-", ".enc");
            try (FileOutputStream fos = new FileOutputStream(output)) {
                fos.write(iv);
                fos.write(ciphertext);
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    public File decryptFile(File encrypted) {
        try {
            byte[] all = Files.readAllBytes(encrypted.toPath());
            if (all.length < IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted file is too short");
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);

            byte[] ciphertext = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            File output = File.createTempFile("cloudnest-dec-", ".tmp");
            try (FileOutputStream fos = new FileOutputStream(output)) {
                fos.write(plaintext);
            }
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /** SHA-256 hex checksum, used to detect unchanged files (Feature 2). */
    public String computeChecksum(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Checksum failed: " + e.getMessage(), e);
        }
    }
}
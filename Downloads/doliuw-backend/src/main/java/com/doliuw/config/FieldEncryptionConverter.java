package com.doliuw.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts sensitive fields
 * using AES-256-GCM. Only ID and email are stored in plaintext — everything
 * else (name, mobile, avatar) goes through this converter.
 *
 * If a hacker steals the DB dump they see only ciphertext; without the key
 * (stored in application.properties / env var) they cannot decrypt.
 */
@Converter
@Component
public class FieldEncryptionConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // Key is injected from app.encryption.secret (must be 32 bytes / 256-bit base64)
    private static String encryptionKey;

    @Value("${app.encryption.secret}")
    public void setEncryptionKey(String key) {
        FieldEncryptionConverter.encryptionKey = key;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        try {
            SecretKey key = getKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV to ciphertext, then base64 encode
            ByteBuffer bb = ByteBuffer.allocate(iv.length + encrypted.length);
            bb.put(iv);
            bb.put(encrypted);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            SecretKey key = getKey();
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            ByteBuffer bb = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            bb.get(iv);
            byte[] encrypted = new byte[bb.remaining()];
            bb.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), "UTF-8");
        } catch (Exception e) {
            // Legacy plaintext value stored before encryption was enabled —
            // return as-is so existing users can still log in.
            // Re-encrypts on the next write (e.g. profile update).
            return ciphertext;
        }
    }

    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("Encryption key must be 32 bytes (256-bit) base64-encoded");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
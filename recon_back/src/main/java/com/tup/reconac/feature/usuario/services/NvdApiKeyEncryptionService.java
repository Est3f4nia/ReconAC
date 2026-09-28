package com.tup.reconac.feature.usuario.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de cifrado de API KEY, soporta cambios de versión de cifrado
 */

@Service
public class NvdApiKeyEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String CURRENT_VERSION = "v1";
    private static final String VERSION_SEPARATOR = ":";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH / Byte.SIZE;
    private static final int MIN_ENCRYPTED_LENGTH = IV_LENGTH + TAG_LENGTH_BYTES;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public NvdApiKeyEncryptionService(@Value("${nvd.encryption.key}") String encodedKey) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La clave de cifrado NVD no es Base64 válido", e);
        }

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("La clave AES debe tener 128, 192 o 256 bits");
        }

        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    public String encrypt(String plainText) {

        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("El texto a cifrar no puede estar vacío");
        }

        byte[] encrypted = encryptV1(plainText);
        return CURRENT_VERSION + VERSION_SEPARATOR + Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String encryptedText) {

        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("El texto cifrado no puede estar vacío");
        }

        int separatorIndex = encryptedText.indexOf(VERSION_SEPARATOR);

        try {
            if (separatorIndex < 0) {
                byte[] legacyData = Base64.getDecoder().decode(encryptedText);
                return decryptPayload(legacyData, null);
            }

            String version = encryptedText.substring(0, separatorIndex);
            String payload = encryptedText.substring(separatorIndex + 1);

            if (version.isBlank() || payload.isBlank()) {
                throw new IllegalArgumentException("Formato de cifrado inválido");
            }

            byte[] data = Base64.getDecoder().decode(payload);

            return switch (version) {
                case "v1" -> decryptPayload(data, version);
                default -> throw new IllegalStateException(
                        "Versión de cifrado NVD no soportada: " + version
                );
            };

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Formato de NVD API key cifrada inválido", e);
        }
    }

    private byte[] encryptV1(String plainText) {

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            cipher.updateAAD(CURRENT_VERSION.getBytes(StandardCharsets.UTF_8));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar la NVD API key", e);
        }
    }

    private String decryptPayload(byte[] data, String version) {

        if (data.length < MIN_ENCRYPTED_LENGTH) {
            throw new IllegalStateException("Los datos cifrados son inválidos");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            if (version != null) {
                cipher.updateAAD(version.getBytes(StandardCharsets.UTF_8));
            }

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar la NVD API key", e);
        }
    }
}

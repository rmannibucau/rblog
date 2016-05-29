package com.github.rmannibucau.rblog.configuration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// same as in tomee to get tomee tooling
@ApplicationScoped
public class DefaultDecrypter implements ConfigurationProducer.Decrypter {
    private static final String TRANSFORMATION = "DESede";
    private static final SecretKeySpec KEY = new SecretKeySpec(new byte[]{
        (byte) 0x76, (byte) 0x6F, (byte) 0xBA, (byte) 0x39, (byte) 0x31,
        (byte) 0x2F, (byte) 0x0D, (byte) 0x4A, (byte) 0xA3, (byte) 0x90,
        (byte) 0x55, (byte) 0xFE, (byte) 0x55, (byte) 0x65, (byte) 0x61,
        (byte) 0x13, (byte) 0x34, (byte) 0x82, (byte) 0x12, (byte) 0x17,
        (byte) 0xAC, (byte) 0x77, (byte) 0x39, (byte) 0x19}, "DESede");

    @Override
    public String decrypt(final String val) {
        try {
            final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, KEY);
            return new String(cipher.doFinal(Base64.getDecoder().decode(val.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
        } catch (final InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Can't decrypt " + val, e);
        }
    }

    @Override
    public String prefix() {
        return "Static3DES";
    }
}

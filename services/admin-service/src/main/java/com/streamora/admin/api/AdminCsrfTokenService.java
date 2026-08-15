package com.streamora.admin.api;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Issues signed double-submit CSRF tokens for the isolated administrator cookie scope. */
@Component
public class AdminCsrfTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final byte[] signingKey = randomBytes(32);

    public String issue() {
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24));
        return nonce + "." + sign(nonce);
    }

    public boolean isValid(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null
                || !constantTimeEquals(cookieToken, headerToken)) {
            return false;
        }
        int separator = cookieToken.lastIndexOf('.');
        if (separator <= 0 || separator == cookieToken.length() - 1) {
            return false;
        }
        String nonce = cookieToken.substring(0, separator);
        return constantTimeEquals(cookieToken.substring(separator + 1), sign(nonce));
    }

    private String sign(String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(nonce.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HmacSHA256 must be available", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}

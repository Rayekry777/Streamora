package com.streamora.identity.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Signed double-submit CSRF tokens; authentication still relies only on HttpOnly cookies. */
@Component
public class CsrfTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final byte[] signingKey = randomBytes(32);

    public String issue() {
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24));
        return nonce + "." + sign(nonce);
    }

    public boolean isValid(String cookieToken, String headerToken) {
        if (cookieToken == null || headerToken == null
                || !MessageDigest.isEqual(
                        cookieToken.getBytes(StandardCharsets.UTF_8),
                        headerToken.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        int separator = cookieToken.lastIndexOf('.');
        if (separator < 1 || separator == cookieToken.length() - 1) {
            return false;
        }
        String nonce = cookieToken.substring(0, separator);
        String signature = cookieToken.substring(separator + 1);
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                sign(nonce).getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 must be available", exception);
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] value = new byte[length];
        SECURE_RANDOM.nextBytes(value);
        return value;
    }
}

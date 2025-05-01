package com.bayoumi.util.validation;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SigningUtil {


    /**
     * Signs the given payload using HMAC-SHA256 and encodes both payload and signature in Base64.
     *
     * @param payload    the string object to sign
     * @param signingKey the secret key combined with timestamp
     * @return SignedPayload containing Base64-encoded data and signature
     * @throws Exception on cryptographic errors
     */
    public static SignedPayload signPayload(String payload, String signingKey) throws Exception {
        // Encode JSON data to Base64
        final String encodedData = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        final String HMAC_ALGORITHM = "HmacSHA256";
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

        // Compute signature over the encoded data
        final String signature = Base64.getEncoder().encodeToString(mac.doFinal(encodedData.getBytes(StandardCharsets.UTF_8)));

        return new SignedPayload(encodedData, signature);
    }

    public static class SignedPayload {
        private final String data;
        private final String signature;

        public SignedPayload(String data, String signature) {
            this.data = data;
            this.signature = signature;
        }

        public String getData() {
            return data;
        }

        public String getSignature() {
            return signature;
        }
    }
}

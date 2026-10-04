package com.munazzar.plotline;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* The same end-to-end scheme as the page (share.js): ECDH P-256 between two people's key pairs →
   SHA-256("plotline-wrap-v1" + shared secret) → AES-256-GCM, output base64(iv[12] + ciphertext + tag).
   Used by the background check to hand an item's key to someone who joined Plotline after being invited. */
final class E2E {
    static byte[] b64d(String s) { return java.util.Base64.getDecoder().decode(s); }
    static String b64e(byte[] b) { return java.util.Base64.getEncoder().encodeToString(b); }

    static PrivateKey priv(String pkcs8) throws Exception {
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(b64d(pkcs8)));
    }

    static PublicKey pub(String raw) throws Exception {
        byte[] r = b64d(raw);
        if (r.length != 65 || r[0] != 4) throw new IllegalArgumentException("bad key");
        byte[] x = new byte[32], y = new byte[32];
        System.arraycopy(r, 1, x, 0, 32);
        System.arraycopy(r, 33, y, 0, 32);
        AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
        ap.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec spec = ap.getParameterSpec(ECParameterSpec.class);
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(new BigInteger(1, x), new BigInteger(1, y)), spec));
    }

    static byte[] pairKey(String myPkcs8, String theirRaw) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(priv(myPkcs8));
        ka.doPhase(pub(theirRaw), true);
        byte[] secret = ka.generateSecret();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update("plotline-wrap-v1".getBytes("UTF-8"));
        md.update(secret);
        return md.digest();
    }

    static String seal(byte[] key, byte[] data) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(data), out = new byte[12 + ct.length];
        System.arraycopy(iv, 0, out, 0, 12);
        System.arraycopy(ct, 0, out, 12, ct.length);
        return b64e(out);
    }

    static byte[] unseal(byte[] key, String s) throws Exception {
        byte[] a = b64d(s);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, a, 0, 12));
        return c.doFinal(a, 12, a.length - 12);
    }

    /* lock an item key (base64, 32 bytes) for someone, from the owner's private key */
    static String wrap(String itemKey, String ownerPkcs8, String theirRaw) throws Exception {
        return seal(pairKey(ownerPkcs8, theirRaw), b64d(itemKey));
    }

    static String fp(String raw) throws Exception {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(b64d(raw));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) sb.append(String.format("%02x", h[i] & 0xff));
        return sb.toString();
    }
}

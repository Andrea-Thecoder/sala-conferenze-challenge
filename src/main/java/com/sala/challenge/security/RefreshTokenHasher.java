package com.sala.challenge.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * I refresh token sono segreti ad alta entropia generati random, non password scelte
 * dall'utente: a differenza di {@link PasswordEncoder} (BCrypt, lento e salato apposta)
 * qui serve un hash deterministico e veloce, perché la ricerca nel DB è per uguaglianza
 * (WHERE token_hash = ?) — BCrypt non lo permetterebbe, produce output diverso ad ogni
 * chiamata anche sullo stesso input. SHA-256 esadecimale è lungo 64 caratteri, combacia
 * con la colonna {@code token_hash} di AppRefreshToken.
 */
public class RefreshTokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

package com.sala.challenge.security;

import io.quarkus.elytron.security.common.BcryptUtil;

/**
 * Wrapper su BcryptUtil (quarkus-elytron-security-common): incapsula l'algoritmo
 * di hashing scelto, così AuthService/UserRepository non dipendono direttamente
 * da BCrypt — se in futuro si passa ad Argon2 cambia solo questa classe.
 *
 * Metodi static: il numero di round non è letto qui internamente, viene passato
 * dal chiamante (che lo ottiene da AuthConfig via @Inject), così questa classe
 * resta utilizzabile anche fuori da un bean CDI.
 */
public class PasswordEncoder {


    public static String hash(String rawPassword, Integer rounds) {
        return BcryptUtil.bcryptHash(rawPassword, rounds);
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return BcryptUtil.matches(rawPassword, hashedPassword);
    }
}

package com.sala.challenge.security;

import com.sala.challenge.config.AuthConfig;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Wrapper su BcryptUtil (quarkus-elytron-security-common): incapsula l'algoritmo
 * di hashing scelto, così AuthService/UserRepository non dipendono direttamente
 * da BCrypt — se in futuro si passa ad Argon2 cambia solo questa classe.
 *
 * Metodi static, quindi AuthConfig non può arrivare via @Inject (CDI inietta solo
 * su istanze): si recupera con SmallRyeConfig#getConfigMapping, l'equivalente
 * "manuale" della @ConfigMapping, utilizzabile anche fuori da un bean.
 */
public class PasswordEncoder {


    public static String hash(String rawPassword, Integer rounds) {
        return BcryptUtil.bcryptHash(rawPassword, rounds);
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        return BcryptUtil.matches(rawPassword, hashedPassword);
    }
}

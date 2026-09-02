# Prossimi passi

## Refresh token / logout (piano in corso)

- [x] **Punto 1** — Entity `RefreshToken` + `RefreshTokenRepository`/`RefreshTokenRepositoryImpl` su Postgres, via `PanacheRepositoryBase` (fatto, compila)
- [ ] **Punto 2** — `AuthConfig`: `refreshTokenExpirationDays`; access token fisso a **5 minuti** in tutti i profili (togliere l'override `%dev` lasciato a 240)
- [ ] **Punto 3** — `AuthService`:
  - `login()` ritorna coppia access token + refresh token (con `familyId` generato alla prima emissione)
  - `refresh(rawToken)`: valida, ruota (revoca il vecchio, emette nuova coppia stesso `familyId`); se il token presentato è già revocato → **reuse detection**, revoca l'intera `family` (`revokeAllByFamilyId`), utente deve rifare login completo
  - `logout(accessToken, refreshToken)`: revoca il refresh token specifico
- [ ] **Punto 4** — Blacklist access token su Redis: claim `jti` in fase di firma, `logout` scrive `blacklist:{jti}` con TTL = scadenza residua; `ContainerRequestFilter` post-autenticazione controlla il `jti` su ogni richiesta protetta — **fail-closed** se Redis è irraggiungibile (nessun circuit breaker, comportamento identico a un IdP esterno giù)
- [ ] **Punto 5** — Cache-aside Redis davanti a `RefreshTokenRepository`: CDI `@Decorator` che implementa `RefreshTokenRepository`, si inserisce via `@Inject @Delegate` davanti a `RefreshTokenRepositoryImpl` — invalidazione della entry Redis **sincrona** su revoke/rotate (mai affidarsi solo al TTL)

## Nota tecnica

- Al prossimo avvio in `dev` (`./mvnw quarkus:dev`), Hibernate rigenera `V1.0__schema.sql` includendo anche `app_refresh_token` — nessuna migrazione Flyway scritta a mano per la tabella, stesso pattern di `Booking`/`Building`/`ConferenceHall`.

## Resto del progetto (dal task della challenge)

- [ ] Endpoint REST auth: registrazione, login, refresh, logout
- [ ] Repository/Service/Resource per `Building`
- [ ] Repository/Service/Resource per `ConferenceHall`
- [ ] Repository/Service/Resource per `Booking`, incluso il controllo dei conflitti temporali sulle prenotazioni
- [ ] Autorizzazione via `@RolesAllowed` sugli endpoint (organizzatore vs cliente)
- [ ] Test (unit + integration) — al momento in `src/test` ci sono solo i placeholder `GreetingResource*` generati da Quarkus
- [ ] README finale del progetto (quello attuale è ancora il testo della challenge, non la nostra documentazione)

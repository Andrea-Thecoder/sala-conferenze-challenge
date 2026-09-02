# Prossimi passi

## Migrazione ORM: Hibernate/Panache → Ebean (fatto)

- [x] Rimossi `quarkus-hibernate-orm`/`quarkus-hibernate-orm-panache`, aggiunto `io.ebean:ebean` + `ebean-maven-plugin` (goal `enhance`) + `querybean-generator` (annotation processor)
- [x] `EbeanConfig`: produce il bean `Database` bridando `AgroalDataSource` (il datasource già gestito da Quarkus) dentro `DatabaseConfig`
- [x] `CurrentUserProviderImpl`: implementa `io.ebean.config.CurrentUserProvider`, legge il subject dal JWT — alimenta `@WhoCreated`/`@WhoModified` su `AbstractAudit` (sostituisce il vecchio `AuditListener` basato su Hibernate, rimosso)
- [x] `AbstractAudit` ora estende `io.ebean.Model` e usa le annotation native Ebean (`@WhenCreated`/`@WhenModified`/`@WhoCreated`/`@WhoModified`) invece di quelle Hibernate
- [x] `RefreshToken` (model) rinominato in **`AppRefreshToken`** (coerente con la tabella `app_refresh_token` e con `User`); repository rinominati `AppRefreshTokenRepository`/`AppRefreshTokenRepositoryImpl`
- [x] Rimosso `@Transient` (JPA) dai metodi calcolati (`Booking.getBookedHours()`, `AppRefreshToken.isActive()`, ora `RefreshToken.isActive()`... vedi nota tecnica sotto) — non serve con Ebean (persistenza per campo, non per property-access come Hibernate)

**Nota tecnica importante**: `ebean-maven-plugin` (goal `enhance`, fase `process-classes`) **non gira con `mvn compile`/`mvn clean compile` da solo** — il packaging `<packaging>quarkus</packaging>` rimappa il lifecycle Maven e salta quella fase per plugin di terze parti. Gira correttamente solo con `quarkus:dev` (verificato: log mostra `Invoking enhance:15.3.0:enhance` ed `Enhanced .../User.class` ecc.). Se un giorno la build "compila" ma le entity non risultano enhanced (niente metodi sintetici `_ebean_*` nel bytecode), è questo il sospetto numero uno.

**Pattern repository**: niente mixin (`implements PanacheRepositoryBase`) — composizione tramite `@Inject Database database`. Letture via `database.find(...)`, scritture via Active Record (`entity.save(tx)`/`entity.update(tx)`/`entity.delete(tx)`) passando sempre una `io.ebean.Transaction` esplicita (niente `@Transactional`, stesso principio di prima solo senza il wrapper `TransactionScope`, rimosso perché ridondante — `Transaction` di Ebean è già `AutoCloseable` con `commit()`).

**Convenzione naming**: `findXxx` → ritorna `Optional`, risultato assente è normale (`GenericRepository.findById`, `UserRepository.findByEmail`). `getXxx` → lancia eccezione se assente, l'esistenza è assunta (`UserRepository.getById`). Da applicare *ancora* ad `AppRefreshTokenRepository` se in futuro serve una variante throwing.

## Refresh token / logout (piano in corso)

- [x] **Punto 1** — `AppRefreshTokenRepository`/`AppRefreshTokenRepositoryImpl` su Ebean (rifatto da zero dopo il wipe della migrazione) — `save`/`findByTokenHash`/`revoke`/`revokeAllByFamilyId`/`revokeAllByUserId`, tutte le scritture fetch-poi-`entity.save(tx)` (mai bulk update `database.update(Class)...` — non si legano alla `Transaction` passata, bug reale trovato e corretto oggi)
- [x] **Punto 2** — `AuthConfig`: fatto dall'utente (non toccato)
- [x] **Punto 3** — `AuthService`:
  - `login()` → `AuthTokenDTO` con access token JWT + refresh token grezzo (hash SHA-256 salvato in DB, mai il grezzo — vedi `RefreshTokenHasher`), `familyId` nuovo alla prima emissione
  - `refresh(rawToken)` → valida, ruota (revoca il vecchio + `replacedByToken`, emette nuova coppia stessa `familyId`); token già revocato presentato → reuse detection, `revokeAllByFamilyId` + errore; **ricontrolla anche `user.isActive()`/`Role.REVOKED`** sull'utente del token (buco trovato e chiuso oggi: prima un utente disabilitato a metà sessione poteva continuare a rifare refresh indefinitamente)
  - `logout(rawToken)` → revoca il refresh token specifico + blacklist dell'access token corrente (jti)
- [x] **Punto 4** — Blacklist access token su Redis:
  - `AuthService.generateToken()`: aggiunto claim `jti` (`Claims.jti`, non c'è uno shortcut `.jwtId()` nel builder SmallRye — verificato sul bytecode)
  - `AccessTokenBlacklist` (`security/`): due livelli — `blacklistToken(jti, ttl)`/`isTokenBlacklisted(jti)` per singola sessione, `revokeAllTokensForUser(subject, ttl)`/`isTokenIssuedBeforeUserRevocation(subject, iat)` per kill-switch per utente (soglia temporale su `iat`, non un flag booleano — un token emesso *dopo* la revoca, es. nuovo login, resta valido)
  - `AccessTokenBlacklistFilter` (`security/`, `@Provider` globale, `@Priority(AUTHENTICATION + 1)`): controlla entrambi i livelli dopo l'autenticazione JWT. **Fail-closed**: eccezione da Redis → 503, mai passare come se non fosse in blacklist
  - `AuthService.revokeAllSessions(userId)`: kill-switch completo (account compromesso/revocato da admin) — `revokeAllByUserId` su Postgres + `revokeAllTokensForUser` su Redis. **Nessun endpoint REST lo chiama ancora** (nessuna Resource admin esiste)
  - Scrittura Redis sempre *dopo* il commit Postgres, in try/catch separato che logga ma non rilancia — Postgres è la fonte di verità (revoca duratura, 60 min), Redis è rinforzo best-effort per la finestra residua (max 5 min); niente ordine inverso, bloccherebbe la protezione più importante dietro quella meno critica (vedi discussione sul dual-write problem)
- [ ] **Punto 5** — Cache-aside Redis davanti a `AppRefreshTokenRepository`: CDI `@Decorator` che implementa `AppRefreshTokenRepository`, si inserisce via `@Inject @Delegate` davanti a `AppRefreshTokenRepositoryImpl` — invalidazione della entry Redis **sincrona** su revoke/rotate (mai affidarsi solo al TTL). **DA FARE, prossimo step.**

## Ambiente locale

- Postgres + Redis via `docker-compose up -d postgres redis` (cartella `docker-compose/`) — avviati e funzionanti in questa sessione
- `.gitignore` creato, `*.pem` escluso (c'era una chiave privata JWT tracciabile per errore)
- Repo git inizializzato in questa sessione con un commit di baseline

## Resto del progetto (dal task della challenge)

- [ ] Endpoint REST auth: registrazione, login, refresh, logout (nessuna Resource JAX-RS esiste ancora — `AuthService` è pronto e testato solo a compile-time, mai chiamato da un endpoint reale)
- [ ] Repository/Service/Resource per `Building`
- [ ] Repository/Service/Resource per `ConferenceHall`
- [ ] Repository/Service/Resource per `Booking`, incluso il controllo dei conflitti temporali sulle prenotazioni
- [ ] Autorizzazione via `@RolesAllowed` sugli endpoint (organizzatore vs cliente)
- [ ] Test (unit + integration) — al momento in `src/test` ci sono solo i placeholder `GreetingResource*` generati da Quarkus, più `GenerateDbMigration.java` (utility Ebean per generare la migrazione init, non un test)
- [ ] README finale del progetto (quello attuale è ancora il testo della challenge, non la nostra documentazione)
- [ ] Verifica end-to-end mai fatta: avviare `quarkus:dev` con Postgres/Redis su e testare login/refresh/logout con una chiamata reale (curl/Postman) — finora solo compilazione + verifica bytecode enhancement

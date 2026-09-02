# Valutazioni post sviluppo

## Trade-off

### Autenticazione:

Ho scelto un'autenticazione JWT auto-emessa invece di un IdP esterno per mantenere il progetto self-contained, senza
dipendenze da servizi esterni, coerente col tempo stimato della challenge.
In un contesto di produzione reale, valuterei un IdP gestito come Keycloak, per centralizzare policy password, MFA,
rotazione/revoca token e audit degli accessi, funzionalità che qui ho implementato in forma minimale.

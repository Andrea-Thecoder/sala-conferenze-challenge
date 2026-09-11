# Sala Backend Challenge

## Parte 1 - Come avviare il progetto

```bash
docker compose -f docker-compose/docker-compose.yml up -d   # Postgres + Redis
mvn quarkus:dev
```

Swagger UI: `http://localhost:8080/ui/v1/openapi` — API sotto `/api/v1/sala-backend-challenge`.

Test (Postgres/Redis effimeri via Quarkus Dev Services, nessun `docker-compose` richiesto):

```bash
mvn verify
```

---

## Parte 2 - Spiegazioni

### Cosa ho deciso di fare e perché

Backend Quarkus per prenotazione sale, con focus su due punti che in un sistema reale
sono i più rischiosi: la race condition sulla sovrapposizione prenotazioni e la
cancellazione utente in ottica GDPR (soft delete/anonimizzazione, non hard delete).

### Come ho strutturato il database e perché

PostgreSQL + Ebean. Entità: `User`, `Building`, `ConferenceHall`, `Booking`, `AppRefreshToken`.
I ruoli (`ADMIN`/`ORGANIZER`/`CUSTOMER`/`REVOKED`) non sono stati inseriti in una Entity: aggiungere
un ruolo richiederebbe comunque la modifica degli endpoint, quindi una tabella dedicata era eccessiva.
Il punto centrale è un vincolo `EXCLUDE USING gist` su
`Booking` che impedisce a livello di database due prenotazioni sovrapposte sulla stessa
sala — l'unica garanzia davvero atomica sotto concorrenza, affiancata da un controllo
applicativo preventivo per dare un errore leggibile nel caso comune.

### Come ho gestito autenticazione e autorizzazione

JWT firmato con chiave RSA locale, access token di vita breve, parametrizzato, refresh token opaco con
rotazione e reuse detection (se un refresh token già usato viene ripresentato, revoco
l'intera famiglia). Autorizzazione su due livelli: `@RolesAllowed` dichiarativo sulle
Resource, più un controllo applicativo (`JwtInspector`) che impedisce a un CUSTOMER di
accedere a risorse di un altro utente, e ad ADMIN incluso di agire per conto di altri su
operazioni self-only (cambio password, revoca proprie sessioni). Revoca sessione: refresh
token invalidati su Postgres, access token già emessi bloccati via blacklist Redis con
filtro fail-closed (Redis giù → richiesta rifiutata, non lasciata passare).

### Cosa non ho fatto e perché

- Una rete di recupero (outbox transazionale) per la blacklist Redis, che oggi è
  best-effort con retry: l'impatto di un fallimento residuo è limitato nel tempo (il
  token scade comunque entro pochi minuti), e l'outbox è sproporzionato per un servizio
  a singola istanza.

### Compromessi

- **Soft delete/anonimizzazione** dell'utente invece di cancellazione fisica, per preservare
  l'integrità dello storico prenotazioni.
- **Filtro di blacklist fail-closed su Redis**: più sicuro, ma introduce una dipendenza
  critica in più per l'intera autenticazione.
- **JWT auto-emesso invece di un IdP esterno** (es. Keycloak): scelta per mantenere il
  progetto self-contained, senza dipendenze da servizi esterni, coerente con i tempi
  stimati della challenge. In un contesto di produzione reale valuterei un IdP gestito per
  centralizzare policy password, MFA, rotazione/revoca token e audit degli accessi — qui
  implementate in forma minimale.

### Cosa migliorerei con più tempo

- Un vero outbox transazionale se il servizio dovesse scalare su più repliche.
- Valutazione di utilizzo di IdP gestiti come Keycloak.
- Separazione in tabelle dedicate comune/regione per la selezione delle città dove si trovano le varie sale conferenze.

### Stack Tecnologico

- **Quarkus (ultima release) + Java 21**: framework cloud-native, build e avvio rapidi,
  buona developer experience per una challenge con tempistiche contenute.
- **PostgreSQL 18**: scelto come RDB perché le entità (utenti, sale, prenotazioni) hanno
  relazioni ben definite e necessitano di vincoli forti tra loro; offre inoltre
  funzionalità avanzate sfruttate nel progetto (vincolo `EXCLUDE`, gestione nativa di UUID
  e boolean).
- **Docker Compose** per l'ambiente locale (Postgres + Redis).

### Assunzioni

- Login tramite indirizzo email, univoco per utente (vincolo `UNIQUE`).
- Ogni utente ha al più un numero di telefono attivo, anch'esso univoco.

---

## Parte 3 - Task

### 🎯 Obiettivo

Questa challenge serve per valutare il tuo livello come backend developer.

Non è un test da "passare", ma uno strumento per osservare:

- **come affronti un problema;**
- **come prendi decisioni;**
- **come strutturi una soluzione.**

Non esiste una soluzione giusta: esistono scelte diverse, e ci interessa capire come arrivi alle tue.

A differenza di molte challenge tecniche, non troverai istruzioni passo-passo.

L'obiettivo è osservare come ti muovi in uno scenario reale, dove non tutto è definito.

---

### ⚙️ Contesto

Stai lavorando al backend di una piccola piattaforma per la gestione di sale per eventi.

La piattaforma mette in relazione due tipologie di utenti:

- organizzatori, che possono creare e gestire le proprie sale;
- clienti, che possono consultare le sale disponibili e richiederne la prenotazione.

Il sistema deve quindi gestire utenti, ruoli, sale e prenotazioni.

Alcuni aspetti non sono completamente definiti.

Come in un contesto reale, dovrai gestire ambiguità e prendere decisioni.

---

### 🧠 Approccio richiesto

Non è necessario completare tutto.

È più importante:

- capire cosa fare;
- decidere cosa non fare;
- motivare le scelte.

Se qualcosa non è chiaro, puoi fare delle assunzioni: esplicitarle è parte della valutazione.

La capacità di prioritizzare e gestire le ambiguità è parte centrale della challenge.

---

### 📌 Minimo atteso (per orientarti)

Se non sai da dove partire, una soluzione base potrebbe includere:

- database con utenti, sale e prenotazioni;
- gestione dei ruoli;
- autenticazione;
- API per creare e consultare le sale;
- API per creare e consultare le prenotazioni;
- autorizzazione delle operazioni in base al ruolo;
- validazione degli input;
- gestione degli errori.

Il resto è a tua discrezione.

---

### 🚀 Come partecipare

1. Crea un fork del repository.
2. Lavora sulla tua copia.
3. Completa la challenge.
4. Invia il link alla tua repository GitHub.

---

### 🧩 Task

Costruisci un piccolo backend per gestire sale per eventi e relative prenotazioni.

#### 1. Database & Data Modeling

Progetta un database che permetta di rappresentare almeno:

- utenti;
- ruoli;
- sale;
- prenotazioni.

Definisci le relazioni necessarie tra le entità.

Puoi scegliere liberamente:

- database relazionale o non relazionale;
- struttura delle tabelle/collezioni;
- chiavi e vincoli;
- eventuali campi aggiuntivi.

Non è necessario costruire un sistema complesso: il modello deve essere coerente con il problema.

#### 2. Authentication & Authorization

Implementa un sistema di autenticazione e gestione dei ruoli.

Il sistema deve distinguere almeno tra:

- organizzatore;
- cliente.

Prevedi API protette e fai in modo che le operazioni disponibili dipendano dal ruolo dell'utente autenticato.

Ad esempio:

- un organizzatore può creare e gestire le proprie sale;
- un cliente può consultare le sale e creare prenotazioni;
- un utente non autenticato non può accedere alle operazioni protette.

La gestione dei permessi deve essere effettuata lato backend.

#### 3. API Design

Definisci le API necessarie per utilizzare il sistema.

La soluzione dovrebbe permettere almeno di:

- creare una sala;
- visualizzare le sale disponibili;
- creare una prenotazione;
- visualizzare le prenotazioni dell'utente.

Puoi scegliere liberamente:

- struttura delle rotte;
- metodi HTTP;
- formato delle response;
- organizzazione del codice.

Le API devono essere coerenti e avere un comportamento prevedibile.

#### 4. Security, Validation & Error Handling

Gestisci correttamente gli input ricevuti dal client.

Prevedi almeno:

- validazione dei dati principali;
- gestione delle richieste non autorizzate;
- gestione degli errori;
- response appropriate in caso di dati non validi.

Considera inoltre eventuali problemi di sicurezza derivanti dalla possibilità di accedere o modificare dati appartenenti
ad altri utenti.

#### 5. Prenotazioni e conflitti temporali

Una sala non può essere prenotata se risulta già occupata nello stesso intervallo temporale.

Per esempio, se una sala è prenotata dalle 18:00 alle 20:00, non deve essere possibile creare una nuova prenotazione
incompatibile con quella esistente.

La verifica deve essere effettuata dal backend.

Gestisci inoltre correttamente i casi limite che ritieni rilevanti.

Non è necessario implementare un sistema avanzato di calendario: è sufficiente dimostrare che il vincolo di
disponibilità viene rispettato.

---

### 📌 Nota importante

Non conta solo che le funzionalità siano presenti.

Verrà valutato anche:

- **come hai progettato il database;**
- **come hai strutturato le API;**
- **come hai gestito autenticazione e autorizzazione;**
- **come hai affrontato sicurezza, validazione ed errori;**
- **come hai organizzato il codice;**
- **come hai gestito il vincolo temporale delle prenotazioni.**

Una soluzione tecnicamente funzionante ma fragile, poco sicura o difficile da mantenere sarà quindi valutata di
conseguenza.

---

### 🧠 Prima di iniziare

Scrivi brevemente (3–5 righe):

- cosa faresti per primo;
- cosa ritieni più importante;
- eventuali dubbi o ambiguità.

Non serve essere esaustivi: ci interessa il tuo processo di ragionamento.

> Partirei dal data model (utenti/ruoli/sale/prenotazioni) e dal vincolo di non
> sovrapposizione, perché è la parte con più implicazioni implicite (concorrenza,
> integrità) e condiziona tutto il resto. Ritengo più importante che il vincolo temporale
> sia garantito a livello DB e non solo applicativo, e che l'autorizzazione sia corretta
> anche nei casi limite (self vs admin), più che la quantità di endpoint. Dubbi iniziali:
> quanto approfondire GDPR/soft-delete e concorrenza reale non essendo esplicitamente
> richiesti dal minimo atteso, ma trattandosi degli aspetti più rischiosi in un sistema
> reale ho preferito investirci tempo piuttosto che ampliare la superficie di feature.

---

### 🧠 README finale

Nel README (breve), spiega sinteticamente:

- **cosa hai deciso di fare e perché;**
- **come hai strutturato il database e perché;**
- **come hai gestito autenticazione e autorizzazione;**
- **cosa non hai fatto e perché;**
- **eventuali compromessi;**
- **cosa miglioreresti con più tempo.**

Non è richiesta una documentazione lunga: poche righe chiare sono sufficienti.

Assicurati inoltre che il progetto possa essere avviato facilmente seguendo le istruzioni presenti nel README.

---

### ⚙️ Stack

Puoi utilizzare qualsiasi tecnologia backend con cui ti senti a tuo agio.

Puoi scegliere liberamente:

- linguaggio;
- framework;
- database;
- librerie.

La valutazione non si basa sulla tecnologia scelta, ma su come affronti il problema e sulle decisioni che prendi.

---

### ⏱️ Tempo

Tempo stimato per una soluzione base funzionante: **60-90 minuti**.

È normale dedicare più tempo per rifinire ulteriormente struttura, sicurezza o dettagli implementativi.

Non è necessario completare tutto perfettamente.

---

### 📦 Output

Invia:

- il link alla repository GitHub;
- il codice funzionante.

---

### 🧠 Valutazione

Riceverai un feedback strutturato su:

- **Database & Data Modeling**
- **Authentication & Authorization**
- **API Design**
- **Security, Validation & Error Handling**
- **Code Quality**
- **Thinking**

La valutazione tiene conto sia dell'esecuzione tecnica sia del modo in cui prendi decisioni.

---

### 💬 Feedback

Dopo la challenge potrai lasciare un feedback su:

- difficoltà;
- chiarezza;
- realismo rispetto a un colloquio backend.

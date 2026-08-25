# Naxos Backend Challenge

## 🎯 Obiettivo

Questa challenge serve per valutare il tuo livello come backend developer.

Non è un test da "passare", ma uno strumento per osservare:

- come affronti un problema;
- come progetti una soluzione;
- come strutturi dati, API e autorizzazioni;
- come prendi decisioni.

Non esiste una soluzione unica: esistono scelte diverse, e ci interessa capire come arrivi alle tue.

A differenza di molte challenge tecniche, non troverai istruzioni passo-passo.

L'obiettivo è osservare come ti muovi in uno scenario reale, dove non tutto è definito.

---

## ⚙️ Contesto

Stai lavorando al backend di una piccola piattaforma per la gestione di sale per eventi.

La piattaforma mette in relazione due tipologie di utenti:

- **organizzatori**, che possono creare e gestire le proprie sale;
- **clienti**, che possono consultare le sale disponibili e richiederne la prenotazione.

Il sistema deve quindi gestire utenti, ruoli, sale e prenotazioni.

Alcuni aspetti non sono completamente definiti.

Come in un contesto reale, dovrai fare delle scelte progettuali e, quando necessario, esplicitare le tue assunzioni.

---

## 🧠 Approccio richiesto

Non è necessario completare tutto.

È più importante:

- capire cosa fare;
- decidere cosa non fare;
- motivare le scelte;
- progettare correttamente le relazioni tra i dati;
- considerare sicurezza e autorizzazioni;
- gestire i casi limite più importanti.

Se qualcosa non è chiaro, puoi fare delle assunzioni: esplicitarle è parte della valutazione.

La capacità di prioritizzare e gestire le ambiguità è parte centrale della challenge.

---

## 📌 Minimo atteso

Se non sai da dove partire, una soluzione base potrebbe includere:

- un database con utenti, sale e prenotazioni;
- gestione dei ruoli;
- autenticazione;
- API per creare e consultare le sale;
- API per creare e consultare le prenotazioni;
- autorizzazione delle operazioni in base al ruolo;
- validazione degli input;
- gestione degli errori.

Il resto è a tua discrezione.

---

## 🚀 Come partecipare

Crea un fork del repository.

Lavora sulla tua copia.

Completa la challenge.

Invia il link alla tua repository GitHub.

---

# 🧩 Task

Costruisci un piccolo backend per gestire sale per eventi e relative prenotazioni.

## 1. Database & Data Modeling

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

---

## 2. Authentication & Authorization

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

---

## 3. API

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

---

## 4. Security, Validation & Error Handling

Gestisci correttamente gli input ricevuti dal client.

Prevedi almeno:

- validazione dei dati principali;
- gestione delle richieste non autorizzate;
- gestione degli errori;
- response appropriate in caso di dati non validi.

Considera inoltre eventuali problemi di sicurezza derivanti dalla possibilità di accedere o modificare dati appartenenti ad altri utenti.

---

## 5. Prenotazioni e conflitti temporali

Una sala non può essere prenotata se risulta già occupata nello stesso intervallo temporale.

Per esempio, se una sala è prenotata dalle 18:00 alle 20:00, non deve essere possibile creare una nuova prenotazione incompatibile con quella esistente.

La verifica deve essere effettuata dal backend.

Gestisci inoltre correttamente i casi limite che ritieni rilevanti.

Non è necessario implementare un sistema avanzato di calendario: è sufficiente dimostrare che il vincolo di disponibilità viene rispettato.

---

## 📌 Nota importante

Non conta solo che le funzionalità siano presenti.

Verrà valutato anche:

- come hai progettato il database;
- come hai strutturato le API;
- come hai gestito autenticazione e autorizzazione;
- come hai affrontato sicurezza, validazione ed errori;
- come hai organizzato il codice;
- come hai gestito il vincolo temporale delle prenotazioni.

Una soluzione tecnicamente funzionante ma fragile, poco sicura o difficile da mantenere sarà quindi valutata di conseguenza.

---

## 🧠 Prima di iniziare

Scrivi brevemente (3–5 righe):

- cosa faresti per primo;
- cosa ritieni più importante;
- eventuali dubbi o ambiguità;
- eventuali assunzioni che hai deciso di fare.

Non serve essere esaustivi: ci interessa il tuo processo di ragionamento.

---

## 🧠 README finale

Nel README finale, spiega sinteticamente:

- cosa hai deciso di fare e perché;
- come hai strutturato il database;
- come hai gestito autenticazione e autorizzazione;
- cosa non hai fatto e perché;
- eventuali compromessi;
- cosa miglioreresti con più tempo.

Non è richiesta una documentazione lunga: poche righe chiare sono sufficienti.

Assicurati inoltre che il progetto possa essere avviato facilmente seguendo le istruzioni presenti nel README.

---

## ⚙️ Stack

Puoi utilizzare qualsiasi tecnologia backend con cui ti senti a tuo agio.

Puoi scegliere liberamente:

- linguaggio;
- framework;
- database;
- librerie.

La valutazione non si basa sulla tecnologia scelta, ma su come affronti il problema e sulle decisioni che prendi.

---

## ⏱️ Tempo

Tempo stimato per una soluzione base funzionante: **45–60 minuti**.

È normale dedicare più tempo per rifinire ulteriormente struttura, sicurezza o dettagli implementativi.

Non è necessario completare tutto perfettamente.

---

## 📦 Output

Invia:

- il link alla repository GitHub;
- il codice funzionante;
- il README con le principali decisioni progettuali.

---

## 🧠 Valutazione

Riceverai un feedback strutturato su:

- Database & Data Modeling;
- Authentication & Authorization;
- API Design;
- Security, Validation & Error Handling;
- Code Quality;
- capacità decisionale.

La valutazione tiene conto sia dell'esecuzione tecnica sia del modo in cui prendi decisioni.

---

## 💬 Feedback

Dopo la challenge potrai lasciare un feedback su:

- difficoltà;
- chiarezza;
- realismo rispetto a un colloquio backend;
- chiarezza dei requisiti;
- tempo necessario per completarla.

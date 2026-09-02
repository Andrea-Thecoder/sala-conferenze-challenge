Naxos Backend Challenge 

Valutazioni iniziali:

Per soddisfare la richiesta del cliente ho selezionato il seguente stack:
- Quarkus last release + java 21
- Postgresql 18 
- Docker per il docker compose che crea in locale il database

Postgresql:
Ho scelto il database relazionale in quanto le entità mostrano una struttura ben definita e necessitano di correlazione tra di esse (relazione) da qui la scelta è ricaduta per ovviamente su un RDB. La scelta di Postgresql in quanto è un opensource d'alta affidabilità, sempre aggiornato e con integrate gestioni avanzate come l'async delle query o la gestione degli UUID e dei Boolean.



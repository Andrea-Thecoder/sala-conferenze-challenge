Sala Backend Challenge

Valutazioni iniziali:

Partirei dalle configurazioni di base e dal modellare le entity, per poi procedere con
repository, service e DTO in parallelo, lasciando i test per ultimi (progetto piccolo,
approccio a waterfall) così da non doverli riscrivere in corso d'opera. 
Ritengo più critico
gestire correttamente la race condition sulle prenotazioni sovrapposte e
l'autenticazione/autorizzazione, essendo le aree con più margine di errore in un sistema
reale. 
Dubbio principale: non è stato evidenziato un livello di Auth, gestirò la situazione con un JWT Sign custom rispetto all'utilizzo di un provider come Keycloak.


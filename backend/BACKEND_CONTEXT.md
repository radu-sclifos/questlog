# QuestLog — Contesto completo del backend

Documento di riferimento per orientarsi nel BE e fornire contesto agli assistenti di sviluppo.
Ultima ricognizione: 27 ottobre 2026. Descrive il codice presente nel workspace; in caso di divergenza prevalgono i sorgenti. Percorsi relativi alla cartella `backend/`, salvo indicazione diversa.

## 1. Scopo e stato attuale

QuestLog gestisce librerie personali di videogiochi: titolo, stato di avanzamento, voto, possesso, tempo di gioco e note. Il backend espone API REST con persistenza PostgreSQL.

Sono implementati:

- Autenticazione tramite sessione Spring Security, login/logout e token CSRF.
- Lettura del profilo e aggiornamento del nome visualizzato.
- CRUD delle entry con controllo del proprietario.
- Ricerca pubblica della libreria di un utente scelto tramite username, con filtri, ordinamento e paginazione sul database.
- Catalogo `games` con elenco, dettaglio e creazione; al momento separato dalle entry.
- Ricerca JPA generica riutilizzabile, migrazione Flyway e test unitari/integrativi.

Non esistono endpoint di registrazione, recupero password, cambio password, statistiche, preferiti o integrazioni con cataloghi esterni. Il README alla radice è parzialmente superato: dichiara assente l'autenticazione, che invece è presente nei sorgenti.

## 2. Stack e configurazione

| Elemento | Configurazione nel progetto |
| --- | --- |
| Java | 25 |
| Spring Boot | 4.1.2 |
| Artefatto Maven | `com.radusclifos:questlog:0.2.0` |
| HTTP | Spring Web MVC, Jakarta Validation |
| Sicurezza | Spring Security, BCrypt, sessione server |
| Persistenza | Spring Data JPA, PostgreSQL |
| Schema | Flyway, `ddl-auto: validate` |
| Mapping | MapStruct 1.6.3, componente Spring |
| Boilerplate | Lombok; processor `lombok-mapstruct-binding` 0.2.0 |
| Build | Maven Wrapper (`mvnw`, `mvnw.cmd`) |

`src/main/resources/application.yml`:

- Context path: `/api`.
- Nome applicazione: `nome-questlog`.
- Datasource da `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- `spring.jpa.open-in-view: false`: mapping e accessi lazy devono avvenire nel contesto transazionale appropriato.
- Nessuna porta esplicita: normalmente Spring Boot usa 8080.

Non inserire credenziali reali in questo documento. La configurazione corrente non definisce profili dedicati di test o produzione.

## 3. Mappa dei file

Radice Java: `src/main/java/com/radusclifos/questlog/`.

```text
backend/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── BACKEND_CONTEXT.md
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__baseline.sql
├── src/main/java/com/radusclifos/questlog/
│   ├── QuestlogApplication.java
│   ├── GeneratePasswordHash.java     # utility locale, non endpoint
│   ├── config/SecurityConfig.java
│   ├── auth/controller/AuthController.java
│   ├── user/
│   │   ├── User.java
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── repository/UserRepository.java
│   │   └── dto/{UserResponse,UpdateProfileRequest}.java
│   ├── entry/
│   │   ├── Entry.java / EntryStatus.java / EntryOwnership.java
│   │   ├── controller/EntryController.java
│   │   ├── service/EntryService.java
│   │   ├── repository/EntryRepository.java
│   │   ├── mapper/EntryMapper.java
│   │   └── dto/
│   │       ├── EntryDto.java
│   │       ├── EntryPage.java
│   │       ├── EntrySearchCriteria.java
│   │       └── SaveEntryRequest.java
│   ├── game/
│   │   ├── Game.java
│   │   ├── controller/GameController.java
│   │   ├── repository/GameRepository.java
│   │   └── dto/{GameResponse,CreateGameRequest}.java
│   └── shared/search/
│       ├── SearchCriteria.java
│       ├── FilterCriteria.java / FilterOperator.java
│       ├── SortCriteria.java
│       ├── SearchRepository.java / SearchRepositoryImpl.java
│       └── CriteriaSpecificationBuilder.java
└── src/test/java/com/radusclifos/questlog/
    ├── QuestlogApplicationTests.java
    ├── user/{SecurityTests,UserLibraryIntegrationTests}.java
    ├── game/GameControllerTests.java
    ├── entry/
    │   ├── EntryControllerTests.java
    │   ├── EntryMapperTests.java
    │   ├── EntryPermissionTests.java
    │   ├── EntrySearchControllerTests.java
    │   ├── EntryJsonSearchIntegrationTests.java
    │   └── EntryStatusMigrationTests.java
    └── shared/search/
        ├── SearchTestSupport.java
        ├── CriteriaSearchTests.java
        └── CriteriaSearchIntegrationTests.java
```

## 4. Architettura e responsabilità

Flusso principale: richiesta HTTP → filtri di sicurezza → controller e validazione DTO → service transazionale → repository JPA → PostgreSQL → mapper/DTO di risposta.

- `QuestlogApplication` avvia Spring e registra `SearchRepositoryImpl` come classe base dei repository JPA tramite `@EnableJpaRepositories`.
- I package seguono le funzionalità (`user`, `entry`, `game`), con sottocartelle per ruolo.
- `EntryController` delega a `EntryService`; il service gestisce proprietario, transazioni e ricerca; `EntryMapper` converte entità e DTO.
- `UserController` delega a `UserService`; `UserResponse.from` effettua il mapping manuale.
- `GameController` accede direttamente a `GameRepository` e converte manualmente le risposte. Non esiste un `GameService`.
- `UserService` ed `EntryService` hanno transazioni di lettura a livello di classe e transazioni di scrittura sui metodi mutanti.
- Le risposte API usano DTO, senza esporre direttamente `User`, password hash o relazioni JPA.
- Gli errori applicativi espliciti usano `ResponseStatusException`; non è presente un handler globale personalizzato.

## 5. Modello dati e schema

Relazione attuale: `users 1 → N entries`. `games` non ha relazioni con le altre tabelle. Tutti gli ID sono `Long`/`BIGINT` con generazione identity.

### User → users

| Campo Java | Colonna / regola |
| --- | --- |
| `id` | PK |
| `username` | `VARCHAR(40)`, obbligatorio e univoco |
| `displayName` | `display_name VARCHAR(100)`, obbligatorio |
| `passwordHash` | `password_hash VARCHAR(255)`, obbligatorio |
| `enabled` | boolean obbligatorio; default SQL true |
| `createdAt` | `created_at TIMESTAMP WITH TIME ZONE`, obbligatorio |

Il vincolo SQL per username è `^[a-z0-9][a-z0-9_-]{2,39}$`: 3–40 caratteri, minuscole, numeri, underscore e trattino, con primo carattere alfanumerico. Il costruttore abilita l'utente; `rename` applica `strip()` al display name; `disable` disabilita l'account.

### Entry → entries

| Campo | Regole effettive |
| --- | --- |
| `id` | PK |
| `user` | `ManyToOne LAZY`, obbligatorio; FK `user_id`, indice dedicato |
| `title` | obbligatorio, massimo 200 caratteri; `rename` controlla e applica `strip()` |
| `score` | opzionale, `Double`; richiesta validata tra 0 e 10 |
| `notes` | opzionale, massimo 2000 caratteri |
| `status` | enum stringa, obbligatorio; default `UNPLAYED` anche se impostato a null |
| `ownership` | enum stringa opzionale |
| `playtime` | `Integer` opzionale; unità non codificata nel backend |

`EntryStatus`: `BEATEN`, `COMPLETED`, `DROPPED`, `PLAYING`, `UNPLAYED`, `ON_HOLD`, `ENDLESS`.

`EntryOwnership`: `OWN`, `HOUSEHOLD`, `FORMERLY_OWNED`, `OTHER`, `SUBSCRIPTION`.

Il database controlla i valori degli enum tramite CHECK. Non ci sono vincoli SQL sul range di score o playtime. `SaveEntryRequest` non applica `@Min(0)` a playtime, anche se tale annotazione compare su `EntryDto`: non assumere che la scrittura rifiuti valori negativi.

### Game → games

Campi: `id`, `title` obbligatorio (200), `coverUrl` opzionale (`cover_url`, 2048). Nessuna FK da Entry a Game: ogni entry mantiene il proprio titolo.

`Game.rename` verifica il titolo usando anche `strip()` per la lunghezza ma salva la stringa originale; differisce da `Entry.rename`.

### Migrazioni

È presente `V1__baseline.sql`, che crea tutte e tre le tabelle. Hibernate valida lo schema senza aggiornarlo. Per modifiche a database già migrati aggiungere una nuova migrazione versionata; modificare una migrazione già applicata può invalidarne il checksum.

## 6. API HTTP

Tutti i percorsi della tabella includono il context path `/api`.

| Metodo | Percorso | Input / output | Accesso attuale |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | `{headerName, parameterName, token}` | pubblico |
| POST | `/api/auth/login` | form `username`, `password`; 204 oppure 401 per credenziali errate | pubblico, con CSRF |
| POST | `/api/auth/logout` | 204 | sessione e CSRF nel normale flusso autenticato |
| GET | `/api/me` | `UserResponse` | autenticato |
| PATCH | `/api/me` | `{displayName}` → `UserResponse` | autenticato, CSRF |
| GET | `/api/users/{username}` | `UserResponse` | autenticato |
| POST | `/api/entries/search` | `EntrySearchCriteria` → `EntryPage` | pubblico, esente CSRF |
| GET | `/api/entries/{id}` | `EntryDto` | proprietario autenticato |
| POST | `/api/entries` | `SaveEntryRequest` → `EntryDto`, 201 | autenticato, CSRF |
| PUT | `/api/entries/{id}` | `SaveEntryRequest` → `EntryDto` | proprietario, CSRF |
| DELETE | `/api/entries/{id}` | 204 | proprietario, CSRF |
| GET | `/api/games` | lista di `GameResponse` | fuori dalla chain configurata |
| GET | `/api/games/{id}` | `GameResponse` | fuori dalla chain configurata |
| POST | `/api/games` | `CreateGameRequest` → `GameResponse`, 201 | fuori dalla chain configurata |

Non esiste un handler GET per l'elenco `/api/entries`: usare la ricerca POST. Non esiste la vecchia ricerca `/api/users/{username}/entries`.

`UserResponse`: solo `username`, `displayName`.

`SaveEntryRequest`: `title`, `score`, `notes`, `status`, `ownership`, `playtime`. L'ID e il proprietario non sono campi modificabili della richiesta. Il PUT sostituisce i campi editabili; i null cancellano i valori opzionali e riportano status a `UNPLAYED`.

`EntryDto`: gli stessi campi editabili più `id`. `GameResponse`: `id`, `title`, `coverUrl`.

Errori principali: 400 per input/criteri non validi, 401 per mancata autenticazione sulle rotte protette, 403 per CSRF non valido sulle richieste soggette al controllo, 404 per risorse mancanti o entry non appartenenti all'utente. Il formato del body di errore non è personalizzato dal progetto.

## 7. Autenticazione, sessione e permessi

`SecurityConfig` carica gli utenti da `UserRepository.findByUsername`, usa `passwordHash`, assegna ruolo `USER` e disabilita l'accesso agli account con `enabled=false`.

Il login è gestito dal filtro Spring Security, non da un metodo del controller. Usa form URL encoded e crea una sessione: il client deve conservare il cookie di sessione. Non sono implementati JWT.

Flusso client:

1. GET `/api/auth/csrf`, conservando il cookie e il token.
2. POST `/api/auth/login` con form e header indicato da `headerName`.
3. GET `/api/me` per recuperare il profilo.
4. Dopo login/logout recuperare un token valido prima delle successive scritture: il ciclo di autenticazione può invalidare quello precedente.
5. Inviare cookie di sessione e CSRF per le operazioni protette mutanti.

La chain è limitata a `/auth/**`, `/me`, `/users/**`, `/entries`, `/entries/**` (percorsi interni senza `/api`). `/games/**` non rientra nella chain e non è presente una seconda chain generale: attualmente queste rotte non ricevono i controlli di autenticazione/CSRF definiti qui.

`UserService.current` verifica che Principal esista e che l'utente sia ancora abilitato, altrimenti 401. `find` restituisce solo utenti abilitati, altrimenti 404.

`EntryService.owned` cerca con `findByIdAndUserId`: leggere, modificare o cancellare un'entry altrui restituisce 404. La creazione assegna sempre l'utente autenticato.

La ricerca pubblica è intenzionalmente indipendente dal visitatore: restituisce la libreria dell'username richiesto, inclusi i campi di `EntryDto` come notes. Non è implementato un flag di visibilità privata.

## 8. Ricerca e paginazione condivisa

Esempio completo:

```http
POST /api/entries/search
Content-Type: application/json
```

```json
{
  "username": "alice",
  "pageNumber": 0,
  "pageSize": 100,
  "filters": [
    {"field": "title", "operator": "CONTAINS", "value": "Hollow"},
    {"field": "status", "operator": "IN", "valueList": ["PLAYING", "UNPLAYED"]}
  ],
  "sorts": [{"field": "score", "direction": "DESC"}]
}
```

Esempio di struttura della risposta vuota:

```json
{"items": [], "page": 0, "size": 100, "totalElements": 0, "totalPages": 0}
```

`EntrySearchCriteria` estende `SearchCriteria` aggiungendo username obbligatorio, non blank, massimo 40 caratteri. Un body con solo username usa i default: pagina 0, size 100, nessun filtro, title ASC.

Regole nel codice:

- `pageNumber >= 0`; `pageSize` tra **5 e 100**.
- Massimo 20 filtri e 5 sort richiesti. Le liste null vengono convertite in liste vuote; elementi null non sono accettati.
- Filtri combinati in AND. Non sono previsti OR o gruppi annidati.
- Campi client: qualsiasi attributo JPA basic diretto, senza whitelist; niente relazioni, collezioni o percorsi con punto.
- Testi confrontati e ordinati senza distinzione maiuscole/minuscole.
- Sort ASC/DESC; duplicati rifiutati; ID ASC aggiunto come spareggio se non già richiesto.
- Filtraggio e conteggio precedono la paginazione. Una pagina oltre la fine ha items vuoto e conserva il totale filtrato.
- Ordinamento dei null lasciato a PostgreSQL: ASC null in fondo, DESC null all'inizio.

| Operatore | Payload e significato |
| --- | --- |
| `EQUAL` | `value` compatibile col tipo del campo |
| `CONTAINS` | `value` stringa; sottostringa letterale con escape di `%`, `_`, `!` |
| `GREATER_THAN` | `value`; campo numerico o temporale compatibile |
| `LESS_THAN` | `value`; campo numerico o temporale compatibile |
| `IN` | solo `valueList`, da 1 a 100 elementi non null; `value` ignorato |
| `IS_NULL` | `value` assente o null |

Gli operatori scalari ignorano `valueList`. Il builder converte stringhe enum e valori numerici compatibili, rifiuta numeri non finiti e tipi incompatibili. Non implementa un parser generico di date JSON: il supporto ai tipi temporali non implica conversione automatica da stringa.

Flusso interno:

1. `EntryService.search` controlla username e chiama `validateCriteria` sui criteri client.
2. Risolve l'utente selezionato tramite `UserService.find`.
3. Aggiunge il filtro server `user.id EQUAL <id>`; se mancano sort aggiunge title ASC.
4. `SearchRepositoryImpl.searchWithCriteria` costruisce una Specification e un PageRequest.
5. `CriteriaSpecificationBuilder` valida metadati/tipi e costruisce predicati e ordine; non applica ORDER BY alla query di count.
6. `EntryMapper.toDtoPage` restituisce `EntryPage`.

I percorsi annidati singolari come `user.id` sono ammessi nel motore interno, dopo la validazione dei criteri client. La validazione client non esegue query SQL. I criteri sono mutati dal service quando aggiunge il filtro del proprietario: non riutilizzare la stessa istanza come richiesta indipendente.

Attenzione al limite: il builder riapplica il massimo di 20 filtri anche dopo l'aggiunta di `user.id`; una richiesta con 20 filtri client può quindi fallire nella seconda validazione. È una discrepanza attuale, non una regola desiderata.

`EntryRepository` e `GameRepository` estendono `SearchRepository<T, ID>`. `UserRepository` estende `JpaRepository`. Per adottare il motore in un altro repository basta estendere l'interfaccia condivisa; nessuna entity class viene passata dai service. Aggiungere un attributo basic a un'entità può renderlo automaticamente ricercabile: valutare anche l'esposizione API.

## 9. Avvio, build e test

Da PowerShell nella cartella backend, predisporre un database PostgreSQL e le variabili della sessione, quindi avviare:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/questlog'
$env:DB_USERNAME = '<utente-locale>'
$env:DB_PASSWORD = '<password-locale>'
.\mvnw.cmd spring-boot:run
```

Flyway applica le migrazioni e Hibernate valida le entità sullo schema. Il progetto non definisce un container database o un datasource di test dedicato. Usare un database separato per i test integrativi.

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

`package` esegue anche i test. Un sottoinsieme senza avvio del database:

```powershell
.\mvnw.cmd '-Dtest=EntryMapperTests,EntryPermissionTests,EntryControllerTests,EntrySearchControllerTests,GameControllerTests,CriteriaSearchTests,SecurityTests' test
```

Questi comandi sono riferimenti operativi; la creazione di questo documento non ne certifica l'esito.

| Test | Area principale |
| --- | --- |
| `EntryMapperTests` | mapping, titolo, campi opzionali, default status, identità |
| `EntryPermissionTests` | proprietà delle entry e assegnazione utente |
| `EntryControllerTests` | CRUD e validazione HTTP |
| `EntrySearchControllerTests` | payload JSON e criteri ricerca |
| `CriteriaSearchTests` | validazione del motore tramite metadati simulati |
| `GameControllerTests` | API catalogo |
| `SecurityTests` | chain reale con MockMvc e repository simulati |
| `UserLibraryIntegrationTests` | librerie e utenti con contesto Spring/database |
| `CriteriaSearchIntegrationTests` | query, filtri, sort e riuso su più entità |
| `EntryJsonSearchIntegrationTests` | ricerca JSON con persistenza |
| `EntryStatusMigrationTests` | default SQL e nullability dello status |
| `QuestlogApplicationTests` | caricamento del contesto |

I test `@SpringBootTest` usano la configurazione datasource dell'applicazione: richiedono PostgreSQL e le variabili DB. Non è dichiarata una dipendenza H2 o Testcontainers.

## 10. Collegamento con il frontend

Il frontend Angular è nella cartella sorella `../frontend`. `proxy.conf.json` inoltra `/api/**` a `http://localhost:8080`.

Riferimenti principali:

- `../frontend/src/app/features/auth/services/auth.service.ts`: login form, logout e ripristino sessione con `/api/me`.
- `../frontend/src/app/features/auth/auth.interceptor.ts`: integrazione HTTP dell'autenticazione.
- `../frontend/src/app/features/entry/services/entry.service.ts`: chiamate entry.
- `../frontend/src/app/features/entry/models/`: contratti e label del dominio.

Le modifiche a enum, DTO e paginazione richiedono di verificare anche questi contratti. La ricerca deve operare sull'intero insieme lato server; filtrare solo gli items della pagina corrente produce risultati incompleti.

## 11. Guida alle modifiche future

| Intervento | File/aree da consultare |
| --- | --- |
| Campo di Entry | `Entry`, DTO richiesta/risposta, `EntryMapper`, migrazione, test e modelli FE |
| Nuovo stato/possesso | enum Java, CHECK SQL con nuova migrazione, label/modelli FE |
| Permessi o privacy | `SecurityConfig`, `UserService`, `EntryService.owned/search`, test sicurezza |
| Ricerca | `SearchCriteria`, `FilterCriteria`, builder, repository, test condivisi e API |
| Profilo utente | User, DTO, UserService/Controller, migrazione se necessaria |
| Catalogo | Game, GameController/Repository, DTO; valutare esplicitamente la sicurezza |
| Nuovo repository ricercabile | estendere `SearchRepository`, verificare gli attributi basic esposti |

Preservare gli invarianti: proprietario assegnato dal server, accesso puntuale tramite coppia ID/utente, DTO senza dati sensibili, validazione client prima dei filtri interni, ordinamento stabile, status non null e compatibilità tra schema ed enum.

MapStruct usa `unmappedTargetPolicy = ERROR`: aggiungere campi richiede mapping o ignore espliciti. I sorgenti generati sono output di build; modificare il mapper sorgente.

## 12. Limiti e discrepanze da ricordare

- Il README alla radice contiene informazioni superate rispetto ai sorgenti; questo file consolida la precedente documentazione del backend.
- `/games/**` è fuori dalla chain di sicurezza corrente, compresa la creazione.
- La ricerca delle librerie è pubblica e include notes; il dettaglio per ID resta riservato al proprietario.
- Il minimo effettivo di pageSize è 5; il filtro server consuma uno dei 20 filtri accettati dal builder finale.
- Nessuna validazione `@Min(0)` su playtime nella richiesta di scrittura.
- Game ed Entry non sono collegati; non assumere un catalogo normalizzato delle librerie.
- Nessun endpoint pubblico di creazione account, nessuna configurazione completa di deployment o CORS esplicita nei sorgenti esaminati.
- `GeneratePasswordHash.java` è un'utility locale con input fisso nel sorgente: non è un servizio di gestione account.

Per mantenere utile questo unico riferimento, aggiornarlo insieme a modifiche di endpoint, schema, sicurezza, ricerca, dipendenze o procedure di avvio. Non usarlo come sostituto della verifica del codice prima di una modifica.

# Front-end QuestLog — riferimento per gli agenti

Sintesi verificata sul codice il 28 ottobre 2026. Si applica a `frontend/`.
Usare questa mappa per individuare i file pertinenti e leggere solo quelli necessari
al compito. Il codice resta la fonte di verità: aggiornare queste note quando cambiano
architettura o convenzioni, senza trasformarle in un diario delle attività.

## Preferenze esplicite dell'utente

- Preferire i componenti e le classi daisyUI quando disponibili per gli elementi dell'interfaccia.

- Evitare gli attributi ARIA (`aria-*`, inclusi i binding `[attr.aria-*]`).
- L'utente conosce l'importanza dell'accessibilità, ma non intende implementare
  funzionalità dedicate agli screen reader in questo progetto. Non aggiungere
  interventi di questo tipo, inclusi ruoli destinati ai lettori di schermo.
- Gli attributi ARIA e i `role="alert"` preesistenti sono stati rimossi dai template
  su richiesta dell'utente. Non reintrodurli nelle modifiche future.

## Stack e convenzioni osservate

- Angular 21.2, TypeScript 5.9, RxJS 7.8; componenti standalone, senza NgModule.
- File dei componenti senza suffisso `.component`: `entry-card.ts/.html/.scss`.
- DI con `inject()`; stato locale con `signal()` e derivazioni con `computed()`;
  comunicazione con `input()` / `output()`, riferimenti con `viewChild()`.
- Template con `@if` / `@for` e `track`; form con Reactive Forms.
- RxJS per HTTP e flussi di eventi; `takeUntilDestroyed()` per sottoscrizioni
  persistenti, `finalize()` per ripristinare gli stati di caricamento.
- Tailwind CSS 4 + daisyUI 5; tema `night` configurato in `src/tailwind.css`.
  Usare classi e colori semantici esistenti; SCSS locale per layout personalizzati.
- Testi dell'interfaccia in inglese. Asset statici in `public/`, loghi in
  `public/images/branding/`. Le entry ricevono game (id, name, coverImageId) dal catalogo IGDB locale.

## Mappa rapida (percorsi relativi a frontend/)

| Area | File / directory | Responsabilità |
| --- | --- | --- |
| Bootstrap | `src/app/app.config.ts` | Router, HTTP/interceptor, inizializzazione sessione |
| Shell | `src/app/app.html` | Contenitore principale, router outlet, toast globali |
| Rotte | `src/app/app.routes.ts` | `/login`, `/list/:username`, `/game/:id`, redirect iniziale |
| Autenticazione | `src/app/features/auth/` | Login, servizio sessione, interceptor, guard |
| Libreria | `src/app/features/entry/pages/entry-list/` | Ricerca, paginazione, permessi UI, refresh dopo CRUD |
| Componenti entry | `src/app/features/entry/components/` | Navbar, header filtri, card espandibili, dialog editor/eliminazione, badge stato |
| API entry | `src/app/features/entry/services/entry.service.ts` | Ricerca e CRUD |
| Modelli entry | `src/app/features/entry/models/` | DTO, enum, etichette e pagina risultati |
| Ricerca condivisa | `src/app/shared/search/` | Filtri, ordinamento, criteri paginati |
| UI condivisa | `src/app/shared/badge/`, `src/app/shared/toast/` | Badge base, notifiche e relativo servizio |

## Comportamenti da preservare

- `/list/:username` è pubblica; i controlli di modifica sono mostrati solo quando
  `AuthService.user()?.username` coincide con il parametro della rotta.
  Questo controllo UI non sostituisce le autorizzazioni del backend.
- `/` reindirizza alla libreria dell'utente autenticato oppure a `/login`.
  `authGuard` esiste, ma non è attualmente collegato alle rotte.
- `AuthService` recupera `/api/me` all'avvio con richiesta condivisa e timeout di
  8 secondi; distingue sessione anonima (401) da servizio non disponibile.
  Login form-urlencoded su `/api/auth/login`, logout su `/api/auth/logout`.
- L'interceptor opera su `/api/`: prima di ogni mutazione recupera un token fresco
  da `/api/auth/csrf`; `POST /api/entries/search` è escluso. Gestisce i 401 con
  eccezioni per login, profilo e ricerca pubblica. Non sostituire questo flusso con JWT.
- Login: `returnUrl` accettato solo con prefisso `/list/`; altrimenti apre la
  libreria dell'utente. La password viene ripulita dopo il successo.
- Ricerca entry: `POST /api/entries/search` con `EntrySearchCriteria(username)`;
  filtri e ordinamenti sono eseguiti dal backend; il nome usa il percorso game.name. `switchMap` sostituisce richieste
  precedenti. Pagina iniziale 0, dimensione UI 25; cambio filtri azzera la pagina.
  Dopo eliminazione dell'ultimo elemento si recupera l'ultima pagina valida.
- Layout libreria: navbar laterale sinistra; paginazione sticky verticale su desktop,
  fissa in basso su mobile. `ResizeObserver` aggiorna l'altezza per il centraggio.
- Editor e conferma eliminazione usano `<dialog>` nativi (`showModal()` / `close()`).
  Durante il salvataggio/eliminazione sono bloccati invii ripetuti e chiusura.
- Entry: titolo obbligatorio non vuoto, massimo 200 caratteri; voto opzionale 0–10;
  playtime opzionale intero non negativo; note massimo 2000 caratteri.
  Il testo per i suggerimenti usa trim(); note vuote inviate come null.
  Stato e possesso sono enum opzionali; etichette in `entry-labels.ts`.
- I dialog emettono eventi al padre, che ricarica la ricerca corrente e mostra toast.

## Comandi e verifiche

Eseguire dalla directory `frontend/`:

- `npm start`: server Angular su porta 4200; proxy `/api/**` verso `localhost:8080`.
- `npm run build`: build production; budget iniziale 500 kB warning / 1 MB errore,
  SCSS per componente 4 kB warning / 8 kB errore.
- `npm run watch`: build continua di sviluppo.
- `npm test` è dichiarato, ma `angular.json` non contiene un target `test` e non
  sono presenti file spec nel sorgente attuale. Non presumere una suite configurata.
- Per modifiche solo documentali non serve compilare. Per modifiche al codice
  eseguire le verifiche pertinenti e riportare eventuali limiti effettivi.

Il README principale è parzialmente obsoleto: autenticazione e librerie per username
sono già implementate. Controllare i sorgenti prima di usare la roadmap come stato attuale.

## Contratto di lettura delle entry

Entry � indipendente da SaveEntryRequest e contiene EntryGame (id, name,
coverImageId). Card, dialog e conferma eliminazione leggono il nome da game.name.
Le cover usano il CDN IGDB con formato t_cover_big, caricamento lazy e fallback
No cover per immagini assenti o non disponibili.
L'editor cerca GET /api/games/suggestions?name=... dopo 3 caratteri e 300 ms.
Una selezione imposta gameId e il nome ufficiale; digitare nuovamente cancella
subito gameId e annulla richieste obsolete. Solo una selezione valida consente
il salvataggio. In modifica il titolo � read-only e i suggerimenti sono disattivati. POST e PUT inviano
gameId e campi personali, senza title. Chiusura e selezione annullano la ricerca.

Il salvataggio dell'editor gestisce HTTP 409 mostrando "This game is already in
your library." e lascia il dialog aperto con i dati inseriti.

La piattaforma di riferimento è opzionale: Entry.platform espone id/name e
SaveEntryRequest.platformId invia la scelta. L’editor carica le piattaforme con
GET /api/games/{id}/platforms dopo la selezione e in modifica, annullando richieste obsolete.
Il backend verifica che la piattaforma appartenga al gioco.

Nell’editor, Status e Reference platform sono affiancati su desktop. Ownership e
Notes sono nella sezione More info, chiusa a ogni apertura; richiuderla conserva i valori.

I pannelli dei menu condividono lo stile globale in src/tailwind.css: dropdown-panel
per menu HTML e select-panel per ::picker(select), senza modificare il campo chiuso.

Achievements: Entry e SaveEntryRequest espongono achievementsUnlocked e achievementsTotal,
interi opzionali non negativi. In More info i due input sono affiancati; se entrambi
indicati, unlocked non può superare total. La card mostra unlocked / total.


## Scheda gioco

`features/games/pages/game-detail/` espone `/game/:id` pubblica e lazy-loaded,
con ID interno e dettaglio da `GET /api/games/{id}`. Il titolo del documento segue
il nome del gioco. ID invalidi e 404 mostrano Game not found; gli altri errori
mostrano un messaggio senza retry manuale. Il cambio rotta annulla richieste precedenti.

Layout: intestazione libera con cover; Your journey; bolla unica con About aperto,
Storyline chiuso e Game details; Related games raggruppati per relazione.
About e Storyline usano details nativi e sono assenti quando privi di testo.
I dettagli hanno due colonne uguali su desktop (piattaforme incluse), una su mobile.
ExpandableText limita i valori a due righe e mostra Show all / Show less solo in caso
di overflow reale, ricalcolato al ridimensionamento. I campi principali restano visibili
anche vuoti con un trattino; Porting e Supporting studios compaiono insieme quando almeno uno dei due è valorizzato;
se entrambi sono vuoti la loro riga è nascosta. Le categorie di relazioni vuote non compaiono.

`game-journey` è visibile solo agli autenticati e cerca esclusivamente l'entry
con username della sessione e filtro game.id, indipendentemente dalla provenienza
della navigazione. Caricamento ed errori sono separati dalla scheda pubblica, senza retry manuale.
Riusa EntryEditorDialog per modifica o aggiunta con gioco preselezionato tramite
openForGame. Dopo il salvataggio usa direttamente l'entry restituita dal dialog; le note non compaiono nella scheda.
`game-cover` gestisce il CDN IGDB e il fallback anche per i giochi correlati.

La cover e il nome in EntryCard sono link a /game/:id. Fermano la propagazione
per evitare di espandere la card durante la navigazione. Il resto della riga e
il pulsante freccia continuano a espandere/richiudere i dettagli; i link non sono
annidati nel pulsante di espansione.

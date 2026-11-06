# QuestLog frontend — agent guide

Updated on November 6, 2026. Applies to `frontend/`.
Use this map to locate relevant files and read only what the task requires.
Source code remains the source of truth: update these notes when architecture or
conventions change, without turning them into an activity log.

## Explicit user preferences

- Prefer daisyUI components and classes when available for UI elements.
- Avoid ARIA attributes (`aria-*`, including `[attr.aria-*]` bindings).
- The user understands the importance of accessibility but does not intend to
  implement dedicated screen-reader support in this project. Do not add such
  changes, including roles intended for screen readers.
- Existing ARIA attributes and `role="alert"` attributes were removed from templates
  at the user's request. Do not reintroduce them in future changes.

## Stack and observed conventions

- Angular 21.2, TypeScript 5.9, RxJS 7.8; standalone components, no NgModules.
- Component filenames omit the `.component` suffix: `entry-card.ts/.html/.scss`.
- Dependency injection with `inject()`; local state with `signal()` and derived
  state with `computed()`; communication with `input()` / `output()`, references with `viewChild()`.
- Templates use `@if` / `@for` with `track`; forms use Reactive Forms.
- RxJS handles HTTP and event streams; use `takeUntilDestroyed()` for persistent
  subscriptions and `finalize()` to reset loading states.
- Tailwind CSS 4 + daisyUI 5; the `night` theme is configured in `src/tailwind.css`.
  Use existing semantic classes and colors; use local SCSS for custom layouts.
- UI text is in English. Static assets live in `public/`, branding in
  `public/images/branding/`. Entries receive game data (id, name, coverImageId) from the local IGDB catalog.

## Quick map (paths relative to frontend/)

| Area | File / directory | Responsibility |
| --- | --- | --- |
| Bootstrap | `src/app/app.config.ts` | Router, HTTP/interceptor, session initialization |
| Shell | `src/app/app.html` | Main container, router outlet, global toasts |
| Global navbar | `src/app/shared/navbar/` | Sticky top bar except on login; session menu with profile, library, and logout |
| Navbar search | `src/app/shared/navbar/components/navbar-search/` | Games/Users, minimum 3 characters, 300 ms debounce, up to 6 results with images; `/api/games/suggestions` and `/api/users/suggestions?name=...&limit=6`; second row below 768 px |
| Routes | `src/app/app.routes.ts` | `/login`, `/list/:username`, `/game/:id`, `/user/:username`, home redirect |
| Authentication | `src/app/features/auth/` | Login, session service, interceptor, guards |
| Library | `src/app/features/entry/pages/entry-list/` | Search, pagination, UI permissions, refresh after CRUD |
| Entry components | `src/app/features/entry/components/` | Filter header, expandable cards, editor/delete dialogs, status badge |
| Entry API | `src/app/features/entry/services/entry.service.ts` | Search and CRUD |
| Shared game suggestions | `src/app/features/games/services/game.service.ts`, `src/app/features/games/components/game-search-result/` | `searchSuggestions` manages threshold, debounce, and states; cover/title/year row shared by navbar (6 results) and editor (5) |
| Entry models | `src/app/features/entry/models/` | DTOs, enums, labels, and result pages |
| Profile | `src/app/features/profile/pages/profile/` | Public page, statistics, currently playing games, and favorites |
| Profile components | `src/app/features/profile/components/` | Profile and favorites editor dialogs |
| Profile API | `src/app/features/profile/services/profile.service.ts` | Profile reads, avatar and favorites management |
| Profile models and utilities | `src/app/features/profile/models/`, `src/app/features/profile/utils/` | Profile contract, avatar URLs, and initials |
| Shared search | `src/app/shared/search/` | Filters, sorting, paginated criteria |
| Shared UI | `src/app/shared/badge/`, `src/app/shared/toast/` | Base badges, notifications, and toast service |

## Behaviors to preserve

- `/list/:username` is public; editing controls appear only when
  `AuthService.user()?.username` matches the route parameter.
  This UI check does not replace backend authorization.
- `/` redirects to the authenticated user's library or to `/login`.
  `authGuard` exists but is not currently attached to routes.
- `AuthService` fetches `/api/me` at startup with a shared request and an 8-second
  timeout; it distinguishes anonymous sessions (401) from service unavailability.
  Login is form-urlencoded at `/api/auth/login`; logout uses `/api/auth/logout`.
- The interceptor handles `/api/`: before each mutation it fetches a fresh token
  from `/api/auth/csrf`; `POST /api/entries/search` is excluded. It handles 401s
  with exceptions for `/api/auth/login`, `/api/me`, and public entry search. Do not replace this flow with JWT.
- Login accepts `returnUrl` only with a `/list/` prefix; otherwise it opens the
  user's library. The password is cleared after success.
- Entry search uses `POST /api/entries/search` with `EntrySearchCriteria(username)`.
  Filtering and sorting run on the backend; game names use `game.name`.
  `switchMap` replaces previous requests. The initial page is 0 and UI page size
  is 25; filter changes reset the page. Deleting the last item on a page triggers
  a request for the last valid page.
- Library layout: Add a game appears beside Your library only for the owner;
  no sidebar. Pagination is vertically sticky on desktop and fixed at the bottom
  on mobile. `ResizeObserver` updates its height for centering.
- The editor and delete confirmation use native `<dialog>` elements
  (`showModal()` / `close()`). Repeated submissions and closing are blocked while saving/deleting.
- Entries require a selected game and nonblank title text; rating is optional,
  0–10; playtime is an optional nonnegative integer; notes allow up to 2000 characters.
  Suggestion input uses `trim()`; empty notes are sent as null.
  Status and ownership are optional enum selections; labels live in `entry-labels.ts`.
- The editor emits `savedEntry` with the entry returned by the backend. The library
  reloads the current search; `game-journey` uses the result directly.
  Delete confirmation notifies its parent to refresh and show a toast.

## Commands and verification

Run from `frontend/`:

- `npm start`: Angular server on port 4200; `/api/**` proxies to `localhost:8080`.
- `npm run build`: production build; initial bundle budgets are 500 kB warning /
  1 MB error, and component SCSS budgets are 4 kB warning / 8 kB error.
- `npm run watch`: continuous development build.
- `npm test` is declared, but `angular.json` has no `test` target and the current
  source tree has no spec files. Do not assume a test suite is configured.
- Documentation-only changes do not require a build. For code changes, run relevant
  checks and report actual verification limits.

## Entry contracts and editing

`Entry` is separate from `SaveEntryRequest` and contains `EntryGame` (id, name,
coverImageId). Cards, editor dialogs, and delete confirmation read `game.name`.
Covers use IGDB's CDN with `t_cover_big`, lazy loading, and a No cover fallback
for missing or unavailable images.

The editor calls `GET /api/games/suggestions?name=...` after 3 characters and a
300 ms debounce. Selecting a result sets `gameId` and the official name; typing
again immediately clears `gameId` and cancels outdated requests. Saving requires
a valid selection. When editing, the title is read-only and suggestions are disabled.
POST and PUT send `gameId` and personal fields, without `title`.
Closing the dialog or selecting a game cancels the suggestion search.

On HTTP 409, the editor displays "This game is already in your library." and
keeps the dialog open with the entered values.

The reference platform is optional: `Entry.platform` exposes id/name and
`SaveEntryRequest.platformId` sends the selection. The editor fetches platforms
from `GET /api/games/{id}/platforms` after game selection and when editing,
canceling outdated requests. The backend checks that the platform belongs to the game.

In the editor, Status and Reference platform appear side by side on desktop.
Ownership and Notes are in More info, collapsed each time the dialog opens;
collapsing it preserves entered values.

Menu panels share global styling in `src/tailwind.css`: `dropdown-panel` for HTML
menus and `select-panel` for `::picker(select)`, without changing the closed field.

Achievements: `Entry` and `SaveEntryRequest` expose `achievementsUnlocked` and
`achievementsTotal`, optional nonnegative integers. Their inputs appear side by
side in More info; unlocked cannot exceed total when both are provided.
The card displays unlocked / total.

## Game detail page

`features/games/pages/game-detail/` provides the public, lazy-loaded `/game/:id`
route, using the internal ID and `GET /api/games/{id}`. The document title follows
the game name. Invalid IDs and 404s show Game not found; other errors show a message
without a manual retry control. Route changes cancel previous requests and scroll
to the top through `ViewportScroller`, including navigation between related games
within the same component.

The page contains the cover, Your journey, descriptions, details, and related games.
About and Storyline use native `details` and are omitted when text is absent.
`ExpandableText` shows Show all / Show less only when content actually overflows,
recalculating on resize. Empty relation categories are omitted.

`game-journey` is visible only to authenticated users and searches exclusively for
the entry matching the session username and `game.id`, regardless of navigation
origin. Loading and errors are separate from public game details, without a manual
retry control. It reuses `EntryEditorDialog` for editing or adding a preselected
game through `openForGame`. After saving, it uses the returned entry directly;
notes are not displayed in this section. `game-cover` handles the IGDB CDN and
fallbacks for both the main game and related games.

The cover and name in `EntryCard` link to `/game/:id`. They stop propagation to
avoid expanding the card during navigation. The rest of the row and the arrow
button still toggle details; links are not nested inside the expansion button.

The main cover enables `GameCover.zoomable`: it opens a native dialog with daisyUI
modal classes, an IGDB `t_1080p` image, and a darkened backdrop. Escape, the close
button, or an outside click closes it. Related-game covers only navigate.

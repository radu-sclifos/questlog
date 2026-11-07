# Frontend testing

## Setup and commands

Run commands from `frontend/`. Use Node.js 24.15 or newer on the Node 24 line
(the current jsdom dependency also supports Node 22.22.2+ and Node 26+).
The project declares npm 11.19.1 in `package.json`.

```powershell
npm ci
npm test
```

`npm test` runs Angular CLI's `@angular/build:unit-test` builder with Vitest.
In an interactive terminal it watches files and reruns tests when they change.
Tests use jsdom in Node.js: no browser, backend, database, Docker, or credentials
are required. HTTP responses are simulated; no live IGDB or Twitch calls are made.

For a single run that terminates with an exit code:

```powershell
npm test -- --watch=false
```

To run one spec or select tests by their descriptions:

```powershell
npm test -- --watch=false --include="src/app/features/entry/services/entry.service.spec.ts"
npm test -- --watch=false --filter="AuthService"
```

Production compilation is a separate check:

```powershell
npm run build
```

No coverage threshold, end-to-end suite, or frontend CI workflow is configured.

## Organization and conventions

Keep `*.spec.ts` files beside the code they exercise. Group cases with `describe`,
describe observable behavior with `it`, and verify outcomes with `expect`.
Import Vitest APIs explicitly so readers can distinguish them from Angular APIs.
Each test should prepare its own state and avoid depending on execution order.

- For plain logic, call the code directly when Angular injection is unnecessary.
- For injected services, use Angular `TestBed`.
- For HTTP services, register `provideHttpClient()` before
  `provideHttpClientTesting()`. Use `HttpTestingController` to inspect requests
  and flush responses. Verify there are no unmatched requests in `afterEach`.
- For components, create the real standalone component with `TestBed`, interact
  through its template or public API, and use `fixture.whenStable()` after changes.
  Replace dependencies at the service boundary; do not reach into protected or
  private component state solely to make assertions.
- Use Vitest fake timers for debounce and timeout behavior, and restore real
  timers after each test. Avoid real-time sleeps.
- Extract shared fixtures only when reuse makes tests clearer. Use typed DTO
  fixtures to keep test data aligned with frontend contracts.

## Current scope

| Spec | Behaviors |
| --- | --- |
| `entry.service.spec.ts` | Search request and returned page, create/update payloads, conflict propagation |
| `game.service.spec.ts` | Minimum query length, trimming, debounce, result/error state, cancellation through the caller's `switchMap` |
| `auth.service.spec.ts` | Shared startup request, cached session, anonymous versus unavailable service, timeout, login and logout |
| `entry-editor-dialog.spec.ts` | Required game selection, selection invalidation, numeric/notes validation, achievement ordering, save payload, duplicate-submit protection, save event, conflict retry, editing |

The editor tests render its real template and stub its services. Native dialog
`showModal()` and `close()` are replaced only on the test dialog instance because
jsdom does not implement browser modal behavior. These tests do not verify visual
layout, native focus handling, or the browser top layer. Service tests validate
the frontend's HTTP contract, not the real backend's response or authorization.

## References

- [Angular testing overview](https://angular.dev/guide/testing)
- [Angular HTTP testing](https://angular.dev/guide/http/testing)
- [Vitest API](https://vitest.dev/api/)

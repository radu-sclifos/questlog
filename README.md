# QuestLog

QuestLog is a web application for organizing your video game library and keeping track of your gaming experience. Find games, record your progress, and explore your library through a personal profile with summary statistics and favorite titles.

Built as a personal portfolio project, it combines an Angular frontend, a Spring Boot backend, and PostgreSQL. Game information is imported from IGDB into a local catalog through batch jobs.

## Features

- **Game catalog:** search games by name and view dedicated pages with covers, descriptions, platforms, studios, and related games.
- **Personal library:** add, edit, and delete entries linked to catalog games, with duplicate detection and input validation.
- **Progress tracking:** record status, an optional rating from 0 to 10, playtime, reference platform, ownership, achievements, and personal notes.
- **Library browsing:** search, filter, sort, and paginate entries in your own library or another user's public library.
- **Personal profiles:** customize your profile and avatar, choose favorite games, and display currently playing titles and summary statistics.
- **Global search:** find games and other users directly from the navigation bar.
- **Authentication and permissions:** sign in with a session-based account; library and profile changes are restricted to their owner.
- **Catalog synchronization:** import and update IGDB data through Spring Batch jobs, with scheduled synchronization and import recovery support.

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | Angular 21.2, TypeScript, RxJS, Reactive Forms |
| UI | Tailwind CSS 4, daisyUI 5, SCSS |
| Backend | Java 25, Spring Boot 4.1.2, Spring Web MVC |
| Authentication | Spring Security, session-based authentication, CSRF protection |
| Catalog integration | IGDB API, Twitch app authentication, Spring Batch |
| Persistence | Spring Data JPA, PostgreSQL |
| Migrations | Flyway |
| Mapping and utilities | MapStruct, Lombok |
| Build | Angular CLI, npm, Maven Wrapper |

Full dependency versions are defined in `frontend/package.json`, `frontend/package-lock.json`, and `backend/pom.xml`.

## Technical Highlights

- **A monolithic backend with integrated batch processing.** REST APIs and Spring Batch jobs share one Spring Boot application and PostgreSQL database, simplifying deployment and data management for the project's scope. Catalog synchronization runs at 03:00 Europe/Rome to reduce contention during expected quieter hours. API and batch workloads share runtime resources and releases, a tradeoff accepted for the expected workload.

- **Local catalog synchronized with IGDB.** Following [IGDB's recommendation](https://api-docs.igdb.com/), QuestLog imports the game data it uses into PostgreSQL, subject to its catalog filters. Searches and game metadata are served locally, with scheduled updates and import recovery support; cover images remain on IGDB's CDN.

- **Shared game data, personal library entries.** Catalog records are separate from users' progress, ratings, playtime, and notes. Entries reference shared games, while backend validation prevents duplicates within a library and checks that selected platforms belong to the game.

- **User avatars stored in PostgreSQL.** Avatars are validated and normalized to 256 × 256 PNG images, then stored in a dedicated table. This simplifies deployment and backup without separate file storage, at the cost of additional database storage and traffic. Revision identifiers refresh cached images after updates.

- **Responsive search flows.** Angular and RxJS debounce searches and cancel outdated requests as input changes. Library filtering, sorting, and pagination run on the backend.

## Backend Tests

The backend suite combines isolated tests with PostgreSQL integration tests using
Testcontainers and the production Flyway migrations. With Java 25 and Docker
running, execute `./mvnw test` from `backend/` (`.\mvnw.cmd test` on Windows).
No external test database or IGDB credentials are needed.

The backend CI workflow runs tests and packages the application on GitHub Actions.
See [backend testing](backend/docs/TESTING.md) for setup, isolation and commands.

## Project Status

The core features are implemented: catalog integration, authentication, personal libraries, public profiles, game detail pages, and game and user search.

QuestLog is being prepared for its first 1.0 release and a small closed trial. Deployment setup and release documentation are still being finalized. Self-service account registration is not currently available.

### Roadmap

The next milestone focuses on preparing the existing features for use:

1. Finalize the deployment environment and document configuration and startup.
2. Run a small closed trial and address issues found through real use.
3. Publish version 1.0 with updated documentation and release notes.

Further development will be guided by feedback from the trial. No release date has been set.

## Repository Structure

```text
questlog/
├── backend/
│   ├── .mvn/                       # Maven Wrapper configuration
│   ├── src/main/java/              # REST API, security, IGDB integration, and batch jobs
│   ├── src/main/resources/
│   │   ├── application.yml         # Spring Boot configuration
│   │   └── db/migration/           # Flyway migrations
│   ├── src/test/java/              # Backend tests
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
└── frontend/
    ├── public/                     # Static assets
    ├── src/app/
    │   ├── features/auth/          # Sign-in and session management
    │   ├── features/entry/         # Library management
    │   ├── features/games/         # Catalog search and game detail pages
    │   ├── features/profile/       # Public profiles and profile editing
    │   └── shared/                 # Navigation, search models, and shared UI
    ├── angular.json
    ├── package.json
    └── proxy.conf.json             # Development proxy to the backend
```

## Author and License

QuestLog was created by [Radu Sclifos](https://github.com/radu-sclifos) as a personal portfolio project.

This project is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE).

You may download, run, study, and modify the project for personal, noncommercial purposes. Noncommercial redistribution and public forks are also permitted under the license. When sharing the project or a modified version, include the license text or its URL and preserve the required copyright notice identifying the original author. Please clearly identify your own modifications and do not present the original project as your own work.

Commercial use, including selling the software or offering it as a paid service, is not permitted under this license. The license also expressly permits the uses described in its Personal Uses and Noncommercial Organizations sections.

Third-party dependencies and assets remain subject to their respective licenses.

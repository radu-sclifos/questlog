# QuestLog

QuestLog is a web application for organizing your video game library and tracking your progress. For each title, you can record its progress status, rating, playtime, ownership type, and personal notes.

The project is under development and consists of an Angular frontend and a Spring Boot backend, with PostgreSQL for data persistence.

## Features

- View your video game library.
- Add, edit and delete entries through a dedicated form.
- Assign an optional rating from 0 to 10.
- Track progress, ownership and playtime
- Record personal notes.
- Validate input and display notifications about operation results.

## Project Status

Library entry management is implemented. The profile button is present in the UI but does not have any associated behavior yet. The project does not currently include authentication or a complete deployment configuration.

### Roadmap

Future development is planned in the following order:

1. **User accounts and personal profiles**
   - Add user registration, login, and profile management.
   - Give each user a personal game library.
   - Create a personal dashboard with library statistics, including games played, completed, currently playing, and planned.
   - Allow users to mark games as favorites.

2. **External game API integration**
   - Integrate external APIs to retrieve real game titles and cover images.
   - Use this catalog when adding games to a library.

3. **Game exploration pages**
   - Create dedicated pages for individual games with additional information from the integrated catalog.

These features are planned and are not yet available. No release dates have been set.

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | Angular 21.2, TypeScript, RxJS, Reactive Forms |
| UI | Tailwind CSS 4, daisyUI 5, SCSS |
| Backend | Java 25, Spring Boot 4.1.2, Spring Web MVC |
| Persistence | Spring Data JPA, PostgreSQL |
| Migrations | Flyway |
| Mapping and utilities | MapStruct, Lombok |
| Build | Angular CLI, npm, Maven Wrapper |

Full dependency versions are defined in `frontend/package.json`, `frontend/package-lock.json`, and `backend/pom.xml`.

## Repository Structure

```text
questlog/
├── backend/
│   ├── .mvn/                       # Maven Wrapper configuration
│   ├── src/main/java/              # Application and REST API
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
    │   ├── features/entry/          # Library management
    │   └── shared/                 # Shared components and services
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

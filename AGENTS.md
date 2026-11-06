# QuestLog — agent guide

Reviewed on November 6, 2026. Source code and configuration remain the source of
truth; update these guides when contracts or conventions change.

## Orientation

- `frontend/`: Angular application. Read [frontend/AGENTS.md](frontend/AGENTS.md)
  before working in this area.
- `backend/`: Spring Boot application with REST APIs, persistence, and IGDB batch jobs.
  Read [backend/AGENTS.md](backend/AGENTS.md) before working in this area.
- Backend technical documentation lives in `backend/docs/`, linked from the backend guide.
- The README presents the project; agent guides explain how to work on it.

## Shared rules

- Keep changes within the requested scope and preserve existing work.
- Changes to DTOs, enums, and APIs also require checking frontend contracts.
- Never include credentials, tokens, or sensitive data in source code, logs, or documentation.
- For documentation-only changes, check diffs, paths, and consistency with the code;
  no build is required. For code changes, run the relevant checks described in the
  area guide and report any actual verification limits.
- Keep these guides concise: conventions, invariants, and links. Put detailed
  explanations in technical documentation without duplicating them here.

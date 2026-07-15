# CLAUDE.md — Flowtime monorepo

This repository contains two apps for **Flowtime** (an AI smart calendar that auto-schedules tasks, habits, and focus time around fixed commitments):

- **`web/`** — the Next.js 16 / React 19 / Prisma 6 / Turso web app and REST API. This is the source of truth for the data model and scheduling engine. **All web commands run from inside `web/`** (`cd web` first): `npm run dev`, `npm run build`, `npm run db:*`, etc. See **`web/CLAUDE.md`** for the full stack, gotchas, and architecture.

- **`android/`** — a native **Kotlin + Jetpack Compose** client. It is a thin **REST client to `web/`'s `/api/*` endpoints** (single-user, no auth) — the server does all scheduling; the app renders and mutates. DTOs/enums/colors mirror `web/lib/ui.ts` and `web/lib/types.ts`. Build with `cd android && ./gradlew assembleDebug`. Default API base URL is `http://10.0.2.2:3000/` (Android emulator → host `localhost`), configurable via `BuildConfig`.

`.git` lives at the repo root. Each app has its own ignore rules (`web/.gitignore`, plus the root `.gitignore` for Android/Gradle artifacts).

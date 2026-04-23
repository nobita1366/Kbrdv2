# Workspace

## Overview

pnpm workspace monorepo using TypeScript. Each package manages its own dependencies.

## Stack

- **Monorepo tool**: pnpm workspaces
- **Node.js version**: 24
- **Package manager**: pnpm
- **TypeScript version**: 5.9
- **API framework**: Express 5
- **Database**: PostgreSQL + Drizzle ORM
- **Validation**: Zod (`zod/v4`), `drizzle-zod`
- **API codegen**: Orval (from OpenAPI spec)
- **Build**: esbuild (CJS bundle)

## Key Commands

- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-spec run codegen` — regenerate API hooks and Zod schemas from OpenAPI spec
- `pnpm --filter @workspace/db run push` — push DB schema changes (dev only)
- `pnpm --filter @workspace/api-server run dev` — run API server locally

See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.

## Side project: FlexBoard Pro (Android keyboard APK)

`flexboard-pro/` contains a complete native Android Kotlin/Jetpack Compose IME project (separate from the pnpm monorepo). It is built in the cloud via the GitHub Actions workflow at `.github/workflows/android-build.yml` — pushing the repo to GitHub produces downloadable debug + release APK artifacts. Includes 9 modules (IME, Auto-Type, suggestions, themes, fonts, clipboard, macros, multi-language, settings) plus an auto-save sentences feature backed by Room.

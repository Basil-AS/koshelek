# Handoff

TASK_ID: 20261012-0209-koshelek-mvp
STATUS: wip
BRANCH: main (remote), local agent/feat/secure-archive
BASE_COMMIT: 76d2da3a3eb1787336c4227934f940ab4bb14a10
HEAD: e8c35119dc073f414be838c625b8e3a1d20138ef
LAST_VERIFIED_COMMIT: e8c35119dc073f414be838c625b8e3a1d20138ef
PR: #1, #2, #3 merged
PROFILE: full
ACTIVE_SKILLS: using-superpowers, cold-start-resume, brainstorming, toolchain-routing, safe-external-fetch, using-git-worktrees, writing-plans, test-driven-development, verification-before-completion, requesting-code-review
TOOLS_USED: Gortex MCP, gh CLI, Gradle, Android SDK, Rustore downloader repository
UPDATED_AT: 2026-10-12T02:46:00+03:00

## Read first

1. `AGENTS.md`
2. `.agents/reference/UNIVERSAL_AGENT_BOOTSTRAP.md`
3. This task's `TASK.md`
4. This task's `JOURNAL.md`
5. Relevant git diff, tests and PR/CI

## Done

- Public repository created: `https://github.com/Basil-AS/koshelek`.
- Gitflow established: protected `main`, feature branches, PR-only merges, required `verify` status.
- Android Compose foundation and CI/release workflows landed.
- Domain model for five initial retailers, static/dynamic code snapshots, and expiry policy landed.
- AES-256-GCM/PBKDF2 encrypted archive primitive and unit tests landed.
- Local APK research cache downloaded and integrity-checked outside git.

## Current work

The next implementation slice is local persistence and secret storage. Credentials must be encrypted at rest with Android Keystore-backed wrapping; prefer refresh tokens/session cookies over raw passwords. Raw-password storage is an explicit opt-in and must never be logged or included in exports by default.

## Verification evidence

- Local: `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` passed on commits for PR #2 and #3.
- GitHub Actions: PR #1, #2, #3 had two successful `verify` runs each.
- Release workflow exists for tags `v*.*.*`; no release tag has been published yet.

## Uncommitted or running state

- Generated `app/build` and `.kotlin` artifacts are ignored; inspect `git status` before continuing.
- Workstation filesystem is 98% full; avoid unnecessary APK/dependency duplication.

## Risks and blockers

- Gortex index may be partial while warming; use native Gortex MCP for indexed source and report integration failure rather than CLI fallback.
- Retailer auth endpoints/protocols are proprietary and may change. Implement adapters behind explicit contracts and do not copy proprietary app code or secrets.
- No backend is planned; refresh requires the user's network and provider session.

## Exact next action

Create `agent/feat/local-persistence` from `origin/main`; add Room entities/repository and an Android Keystore secret-store abstraction with focused tests delegated to agy, then run full local verification and deliver via PR.

## Completion path

Implement → targeted verification → full relevant verification → Gortex/PR review → GitHub Actions → merge → update task/journal/handoff.

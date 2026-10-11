# Koshelek Android Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the first working Android foundation for a local-first, ad-free loyalty-card wallet with static code display, encrypted archive import/export, tests, and GitHub automation.

**Architecture:** Native Kotlin application with Jetpack Compose UI, a domain model independent of Android UI, Room-backed local persistence, Android Keystore-backed encryption, and explicit adapter boundaries for later retailer integrations. The first phase intentionally ships generic scanned cards; retailer adapters and dynamic QR refresh are subsequent independently testable phases.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Android SDK, Jetpack Compose Material 3, Room, DataStore, AndroidX Security/Keystore APIs, CameraX/ML Kit seam, JUnit, Compose UI tests, GitHub Actions.

## Global Constraints

- Android only; no backend and no mandatory account.
- No ads, analytics, advertising ID, contacts, phone, or location permissions.
- `main` receives changes only through pull requests.
- Work on `agent/*` task branch and use SemVer tags for releases.
- Never store raw credentials in logs; retailer credentials are opt-in and encrypted.
- APKs from retailer apps are behavioral/UX evidence only; do not copy proprietary code or bypass protections.
- Preserve the supplied bootstrap and task-state files.

---

### Task 1: Android project and repository automation

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/ru/basilas/koshelek/MainActivity.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/ui/theme/*`
- Create: `app/src/test/java/ru/basilas/koshelek/BuildSmokeTest.kt`
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/release.yml`
- Modify: `.gitignore`

**Interfaces:**
- Produces an installable `app` module with `debug` and `release` variants.
- CI exposes `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleDebug`.
- Release workflow builds an unsigned artifact unless signing secrets are configured; it must never require secrets for CI validation.

- [ ] **Step 1: Write the failing build smoke test**

Create a JVM test that asserts the application module exposes a non-empty application ID and version name through generated build config.

- [ ] **Step 2: Run the targeted test and observe the expected missing-project failure**

Run: `./gradlew testDebugUnitTest --tests '*BuildSmokeTest'`
Expected: fail because the Gradle project and `app` module do not exist yet.

- [ ] **Step 3: Add the minimal Gradle Android project**

Use a version catalog, Kotlin JVM toolchain matching the installed Android toolchain, Compose compiler/plugin compatible with the selected Kotlin version, min SDK 28, target/compile SDK from the installed SDK, and package `ru.basilas.koshelek`. Keep the manifest permissions to `INTERNET` and `CAMERA` only where needed by the first phase.

- [ ] **Step 4: Add the Compose shell and test implementation**

Create a single `MainActivity` with a Material 3 theme and a placeholder wallet screen. Add the build smoke test implementation and run the test until green.

- [ ] **Step 5: Add CI and release workflows**

CI checks formatting-independent compilation, unit tests, lint, and debug APK assembly. Release workflow triggers on `v*.*.*` tags, uploads the generated artifact, and uses GitHub OIDC/secret signing only when configured.

- [ ] **Step 6: Verify the foundation and commit**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`
Commit: `chore(android): bootstrap koshelek app and github checks`

### Task 2: Domain model and expiry policy

**Files:**
- Create: `app/src/main/java/ru/basilas/koshelek/domain/model/LoyaltyCard.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/domain/model/CardCodeSnapshot.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/domain/model/Retailer.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/domain/refresh/CardRefreshPolicy.kt`
- Test: `app/src/test/java/ru/basilas/koshelek/domain/refresh/CardRefreshPolicyTest.kt`

**Interfaces:**
- `CardCodeSnapshot(payload: String, format: CodeFormat, obtainedAt: Instant, expiresAt: Instant?)`.
- `CardRefreshPolicy.canDisplay(snapshot, now): Boolean`.
- Static snapshots never expire; dynamic snapshots are displayable only until `expiresAt`.

- [ ] **Step 1: Write failing tests for static, valid dynamic, and expired dynamic snapshots**
- [ ] **Step 2: Run `./gradlew testDebugUnitTest --tests '*CardRefreshPolicyTest'` and verify the expected failure**
- [ ] **Step 3: Implement the minimal immutable domain types and policy**
- [ ] **Step 4: Re-run the focused test and verify green**
- [ ] **Step 5: Commit `feat(domain): model loyalty cards and code expiry`**

### Task 3: Local Room persistence

**Files:**
- Create: `app/src/main/java/ru/basilas/koshelek/data/db/KoshelekDatabase.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/data/db/CardEntity.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/data/db/CardDao.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/data/CardRepository.kt`
- Test: `app/src/test/java/ru/basilas/koshelek/data/CardRepositoryTest.kt`

**Interfaces:**
- `CardRepository.observeCards(): Flow<List<LoyaltyCard>>`.
- `CardRepository.upsert(card: LoyaltyCard)`.
- `CardRepository.delete(cardId: String)`.
- Repository maps only domain models and keeps Android database details internal.

- [ ] **Step 1: Write failing repository behavior tests using an in-memory Room database**
- [ ] **Step 2: Run the focused repository test and verify it fails because the repository is absent**
- [ ] **Step 3: Implement entity, DAO, database, and repository mapping**
- [ ] **Step 4: Run repository tests and the complete unit suite**
- [ ] **Step 5: Commit `feat(storage): persist local loyalty cards`**

### Task 4: Encrypted archive import/export

**Files:**
- Create: `app/src/main/java/ru/basilas/koshelek/domain/archive/ArchiveFormat.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/data/archive/EncryptedArchiveCodec.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/data/archive/ArchiveRepository.kt`
- Test: `app/src/test/java/ru/basilas/koshelek/data/archive/EncryptedArchiveCodecTest.kt`

**Interfaces:**
- `EncryptedArchiveCodec.export(cards, password): ByteArray`.
- `EncryptedArchiveCodec.importArchive(bytes, password): List<LoyaltyCard>`.
- Wrong password and tampered ciphertext fail before any database mutation.
- Archive format includes magic bytes, version, salt, nonce, KDF parameters, and AEAD ciphertext.

- [ ] **Step 1: Write failing round-trip, wrong-password, tamper, and empty-archive tests**
- [ ] **Step 2: Run focused archive tests and verify failure before implementation**
- [ ] **Step 3: Implement PBKDF2-HMAC-SHA256 key derivation and AES-GCM codec with random salt/nonce**
- [ ] **Step 4: Re-run archive tests and verify all pass**
- [ ] **Step 5: Commit `feat(archive): add encrypted card import and export`**

### Task 5: Wallet UI and static code display

**Files:**
- Create: `app/src/main/java/ru/basilas/koshelek/ui/wallet/WalletScreen.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/ui/card/CardScreen.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/ui/card/CodeRenderer.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/ui/wallet/WalletViewModel.kt`
- Test: `app/src/androidTest/java/ru/basilas/koshelek/ui/WalletScreenTest.kt`

**Interfaces:**
- Empty wallet offers add/scan/import actions.
- Wallet list renders store name, card title, code freshness, and favorite state.
- Card screen renders a high-contrast code and supports screen-on display.

- [ ] **Step 1: Write Compose tests for empty state, list state, and expired dynamic state**
- [ ] **Step 2: Run Android UI tests and verify failure because screens are absent**
- [ ] **Step 3: Implement minimal Compose screens, preview data, and ViewModel state**
- [ ] **Step 4: Run Compose tests and inspect the debug APK manually**
- [ ] **Step 5: Commit `feat(ui): add wallet and loyalty card screens`**

### Task 6: Scanner seam and adapter framework

**Files:**
- Create: `app/src/main/java/ru/basilas/koshelek/scanner/BarcodeScanner.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/integration/RetailerAdapter.kt`
- Create: `app/src/main/java/ru/basilas/koshelek/integration/RetailerRegistry.kt`
- Create: `app/src/test/java/ru/basilas/koshelek/integration/RetailerRegistryTest.kt`

**Interfaces:**
- Scanner emits `ScannedCode(format, payload)` without persisting automatically.
- Adapter registry resolves known retailer IDs and always exposes a generic fallback.
- No real retailer network calls are included in foundation; fake adapters cover refresh contracts.

- [ ] **Step 1: Write failing registry and scanner-result tests**
- [ ] **Step 2: Verify focused tests fail for missing registry implementation**
- [ ] **Step 3: Implement registry, generic adapter, and scanner abstraction**
- [ ] **Step 4: Run focused and complete tests**
- [ ] **Step 5: Commit `feat(integration): define scanner and retailer adapter contracts`**

### Task 7: Review, PR, and delivery checkpoint

**Files:**
- Modify: `.agents/tasks/20261012-0209-koshelek-mvp/TASK.md`
- Modify: `.agents/tasks/20261012-0209-koshelek-mvp/JOURNAL.md`
- Modify: `.agents/tasks/20261012-0209-koshelek-mvp/HANDOFF.md`
- Modify: `README.md`

- [ ] **Step 1: Run fresh verification**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`

- [ ] **Step 2: Review the diff and scan for secrets/permissions**

Run: `git diff --check`, `git diff --stat`, and a secret scanner available in CI.

- [ ] **Step 3: Request code review against the foundation commits**

Use the review agent with the exact base and head SHAs and fix all critical/important findings.

- [ ] **Step 4: Push only the task branch and create a public GitHub repository/PR with `gh`**

The PR description must include scope, DoD checklist, test logs, security notes, and the fact that retailer APKs were not copied.

- [ ] **Step 5: Merge only after green required checks and review; tag the first release**

Use `gh pr merge --squash --delete-branch` only after verification; create `v0.1.0` release with the unsigned/CI artifact unless signing secrets are configured.

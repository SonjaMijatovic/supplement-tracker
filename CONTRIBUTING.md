# Development Notes — Daily Supplement & Medication Tracker

## Project Setup

### Module naming
The JetBrains KMP wizard generates the shared module as `composeApp`. Rename to `shared` immediately at project creation — before writing any code. Module names embed deeply into import paths, Gradle task names, and the Xcode framework name.

### Module structure
One `:shared` module with internal packages (`data/`, `domain/`, `ui/`). Do not split into `:shared-domain`, `:shared-data`, `:shared-presentation`. Over-modularization adds Gradle wiring overhead with no benefit at this project scale.

### Story 1 checklist
Before any other stories begin, story 1 must deliver:
- Project initialized from KMP wizard with `composeApp` renamed to `shared`
- `DatabaseDriverFactory` (`expect`/`actual`) defined with nullable context signature
- SQLDelight schema stub — at minimum the `items` table skeleton
- iOS first build verified (see iOS build notes below)

---

## DatabaseDriverFactory

The `DatabaseDriverFactory` uses a nullable `Any?` context parameter to support all three initialization contexts: normal Android app, Android `BroadcastReceiver` (YES notification action), and iOS Notification Service Extension (NSE).

```kotlin
// commonMain
expect class DatabaseDriverFactory(context: Any? = null) {
    fun createDriver(): SqlDriver
}
```

- **Android app:** pass `applicationContext`
- **Android BroadcastReceiver:** pass the `Context` received in `onReceive()`
- **iOS / NSE:** call `DatabaseDriverFactory()` — no argument needed

Do not hardcode `applicationContext` into the factory. The BroadcastReceiver and NSE run as separate processes and cannot use the main app context.

---

## iOS Build Notes

### Java PATH for Xcode Build Phases
Xcode runs Build Phase scripts in a shell that may not have Java on `$PATH`. On first iOS build, verify the `embedAndSignAppleFrameworkForXcode` Build Phase script can find Java.

Quick check — temporarily add `which java` as the first line of the Build Phase script. If it returns empty, add to the script header:

```bash
export JAVA_HOME=$(/usr/libexec/java_home)
```

Remove the `which java` debug line once confirmed working.

### After any Kotlin or Compose Multiplatform version upgrade
Xcode caches framework binaries. After a version bump, stale cache causes phantom build failures (missing symbols, linker errors, crashes that don't match current code). Always clear both caches before the first build after an upgrade:

```bash
# From project root — clears Gradle build output
rm -rf build/

# Clear Xcode's DerivedData for this project
rm -rf ~/Library/Developer/Xcode/DerivedData/<YourProjectName>*
```

Or to clear all DerivedData (nuclear option):

```bash
rm -rf ~/Library/Developer/Xcode/DerivedData
```

Alternatively via Xcode GUI: `Product → Clean Build Folder` (⇧⌘K), then `Window → Organizer` to clear DerivedData.

**Rule:** If an iOS build breaks after a Kotlin/CMP version bump, clear both caches first — before spending any time debugging.

---

## iOS Framework Integration

The KMP wizard uses **direct XCFramework embedding** (not SPM). The Xcode project includes a Build Phase script that runs:

```
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

This compiles `:shared` Kotlin code into an XCFramework binary, signs it, and bundles it into the iOS app on every build. This is why iOS builds are slower than Android — the framework recompilation step is heavier than Android's incremental Kotlin compilation.

---

## SQLDelight Schema Notes

Define the schema stub in story 1 — SQLDelight generates Kotlin classes at compile time. Downstream stories (item management, logging, notifications) all import generated classes. Without a `.sq` file, the project does not compile for those stories.

Columns can be added in later stories via explicit migration scripts. SQLDelight migration support exists for this. Do not defer the initial schema stub to avoid blocking story 2+.

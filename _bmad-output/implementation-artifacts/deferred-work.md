# Deferred Work

## Deferred from: code review of 1-1-kmp-project-initialization (2026-04-08)

- No `iosX64` simulator target — Intel Mac developers will get a linker error from `embedAndSignAppleFrameworkForXcode`. Add `iosX64()` to `shared/build.gradle.kts` if Intel Mac support is needed.
- JAVA_HOME no error guard in Xcode build phase script — on machines with no JDK, `$(/usr/libexec/java_home)` exits silently. Add a guard: `if ! JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null); then echo "JDK not found"; exit 1; fi`. Documented in CONTRIBUTING.md.
- No `androidTest` source set despite espresso/testExt declared in the version catalog. Wire up instrumentation tests when Story 1.3 (DI) or later stories introduce logic worth integration-testing.

# 2048 launcher for Insular

This fork keeps Insular’s profile management and internal package identifiers. The complete application and both launcher entries use the name 2048 and a grid icon. The original license and attribution remain in LICENSE and README.md.

## Open Insular

Tap the four corner **cells** in this order: top left, top right, bottom right, bottom left. Then long-press the 2048 title. Do not swipe or press a direction button between these taps. Each step must follow within 10 seconds. Normal play never opens Insular just by reaching a score.

The board, score and best score are saved locally. The game needs no network access. New game asks before replacing the current board. Returning to a stopped launcher activity shows the puzzle again.

## Scope

This is a discreet launcher, not an authentication or encryption mechanism. Android Settings, package inspection, work-profile indicators, existing notification and deep-link flows can still reveal Insular. Internal screens retain the upstream functionality and wording. The management launcher is excluded from Recents; both management launcher activities disable screenshots. Existing internal entry points remain compatible.

The same application ID is retained to avoid breaking hardcoded engine and profile provisioning contracts. An APK signed with a new key cannot update an existing F-Droid installation. Do not remove an existing profile to install an untested build.

## Validation

Verified locally: standalone engine checks pass; PuzzleActivity and PuzzleEngine compile with JDK 11 against the official Android API 30 jar; modified XML files parse and git diff --check passes. Full APK and device provisioning have not yet been verified.

Standalone rules checks:

```sh
mkdir -p build/puzzle-check
javac -d build/puzzle-check mobile/src/main/java/com/oasisfeng/island/puzzle/PuzzleEngine.java tests/puzzle/PuzzleEngineCheck.java
java -cp build/puzzle-check com.oasisfeng.island.puzzle.PuzzleEngineCheck
```

Full APK builds use the upstream Gradle 6.5 / Android Gradle Plugin 4.1 / SDK 30 toolchain and sibling deagle and setup-wizard checkouts. The workflow pins compatible revisions in .github/workflows/android.yml because current deagle dev requires a newer toolchain. Before installing for real, verify: fresh launch into game; normal play and saved game; corner sequence; cancellation/timeout; initial profile setup; automatic and manual provisioning; both profile launchers; rotation and process recreation; background/return; existing shortcuts and notifications.

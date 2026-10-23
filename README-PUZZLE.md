# 2048 launcher for Insular

This fork keeps Insular’s profile management and internal package identifiers. The complete application and both launcher entries use the name 2048 and a grid icon. The original license and attribution remain in LICENSE and README.md.

## Open Insular

Tap the four corner **cells** in this order: top left, top right, bottom right, bottom left. Then long-press the 2048 title. Do not swipe or press a direction button between these taps. Each step must follow within 10 seconds. Normal play never opens Insular just by reaching a score.

The board, score and best score are saved locally. The game needs no network access. New game asks before replacing the current board. Returning to a stopped launcher activity shows the puzzle again.

## Scope

This is a discreet launcher, not an authentication or encryption mechanism. Android Settings, package inspection, work-profile indicators, existing notification and deep-link flows can still reveal Insular. Internal screens retain the upstream functionality and wording. The management launcher is excluded from Recents; both management launcher activities disable screenshots. Existing internal entry points remain compatible.

The same application ID and shared user ID are retained to avoid breaking hardcoded engine and profile provisioning contracts. An APK signed with a new key cannot update an existing F-Droid installation. Do not remove an existing profile to install an untested build.

## Source and validation

Maintained upstream: https://gitlab.com/secure-system/Insular, dev-ci commit 2114d511355075347682effebb09e2f8104fd11d (2026-09-11). This source is newer than F-Droid stable 6.4.2-d46911c9; the fork uses versionName 6.4.2-2048.3, versionCode 51802. It does not claim to be an upstream stable release.

The fork preserves upstream's pinned deagle and setup-wizard submodules. Builds use Java 17, Gradle 8.13, Android Gradle Plugin 8.3.1, compile API 35 and target API 34. No legacy vendored dependencies or old cross-profile patch are carried over. The modern upstream identifies managed profiles through LauncherApps rather than unauthorized cross-user policy queries.

Run scripts/check-puzzle.sh for standalone rules checks. GitHub Actions builds the complete APK and runs the launcher smoke test on Android 14, including an existing unrelated managed profile. Test coverage and actual results are recorded with the delivered artifact; full device provisioning must still be verified on the phone.

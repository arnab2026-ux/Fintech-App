# Portfolio Tracker — scaffold

Step 0 of `portfolio-app-agent-playbook.md`. Module skeleton only. No features.

## Modules

| Module | Type | Depends on |
|---|---|---|
| `:app` | Android app | all modules |
| `:core:model` | pure Kotlin (JVM) | — |
| `:core:money` | pure Kotlin (JVM) | — |
| `:core:database` | Android library (Room + KSP) | `:core:model` |
| `:core:network` | Android library (Ktor + Supabase) | `:core:model` |
| `:feature:portfolio` | Android library (Compose + Hilt) | `:core:model`, `:core:money`, `:domain:valuation` |
| `:domain:valuation` | pure Kotlin (JVM) | `:core:model`, `:core:money` |
| `:domain:tax` | pure Kotlin (JVM) | `:core:model`, `:core:money` |

The four pure-Kotlin modules use the `kotlin-jvm` plugin, not `android-library`.
That is the enforcement mechanism for "no Android deps" — an Android dependency
will not resolve there. Do not convert them to Android modules.

## What is actually in here

- `:app` — `PortfolioApplication` (`@HiltAndroidApp`), `MainActivity`
  (`@AndroidEntryPoint`), a placeholder Material 3 theme.
- `:feature:portfolio` — `PortfolioRoute()`, a blank `Scaffold`. This is the
  screen you see when you run the app.
- Everything else — build files and empty source trees. Dependencies are wired
  and ready (Room + KSP in `:core:database`, Ktor + Supabase in `:core:network`)
  but no entities, DAOs, clients, or domain types are written yet.

## Toolchain

- JDK 17, `compileSdk` 35, `minSdk` 26, `targetSdk` 35
- Kotlin 2.1.20, AGP 8.9.1, Gradle 8.13, KSP, Hilt 2.56.1
- Compose BOM, Material 3, Room 2.7.1, Ktor 3.1.2, supabase-kt 3.1.4

`minSdk` is 26 so `java.time` (`LocalDate`, `Instant`) is available without core
library desugaring — the playbook's date rule depends on those types.

All versions live in `gradle/libs.versions.toml`. Per the house rules that come
in Step 2, agents must not edit that file without asking.

## First build

The Gradle wrapper JAR is **not** committed here — it is a binary, and this
machine has no JDK/Gradle to generate it with. Do one of:

- Open the project in Android Studio. On first sync it will offer to create the
  wrapper, or use its bundled Gradle. Accept.
- Or, with Gradle 8.13 on your PATH: `gradle wrapper --gradle-version 8.13`

You also need `local.properties` pointing at your Android SDK. Android Studio
writes it on first sync; otherwise create it yourself:

```
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Then:

```
./gradlew assembleDebug
```

## Verify before Step 1

```
./gradlew build
./gradlew :app:installDebug
```

Expect a blank screen with the app name in the launcher.

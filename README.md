# XO3D
Native Android tic-tac-toe built with Kotlin and Jetpack Compose.

Modules: app (UI), engine (rules and Minimax AI), bluetooth (message protocol/session foundation).

## Build
Requires JDK 17. Gradle 8.9 is downloaded by the included launcher on first use.
```sh
sh ./gradlew test
sh ./gradlew :app:assembleDebug
```

## Implementation status
Initial foundation: solo/local play, a perspective-styled board, Minimax difficulty levels, Bluetooth message protocol foundation, unit tests, and CI. Real Bluetooth socket discovery/connection, persistent recovery, production 3D model rendering, and two-device integration testing remain subsequent milestones.

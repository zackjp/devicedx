# Project: DeviceDx - Android (Jetpack Compose)

Real-time Android network diagnostics app (traffic, latency, WiFi monitoring).
Single `app/` module Kotlin + Jetpack Compose + Clean Architecture.
Read this before writing any code.

## Tech Stack

For specific versions, please check `libs.versions.toml` file.

- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM with Clean Architecture
- Dependency Injection: Hilt
- Navigation: Compose Navigation 3 (accept tradeoff of using alpha)
- Database: Room
- Async: Coroutines + Flow

## Compose Coding Standards
- **State Hoisting**: Always hoist state to the ViewModel or a stateless Composable wrapper.
- **Naming**: Composables that return Unit must be PascalCase; those that return values should be camelCase.
- **Performance**: Use `remember` and `derivedStateOf` for expensive calculations. Mark domain models as `@Stable` or `@Immutable`.
- **Modifiers**: Pass a `modifier: Modifier = Modifier` as the first optional parameter to all UI Composables.
- **Scaffold**: Use `Scaffold` for top-level screens to handle TopBar and PaddingValues correctly.

## Core architecture decisions
- Expose a single `StateFlow<ScreenState>` from ViewModels.
- Avoid collecting `Flow`'s in the ViewModel's `init` function.
- Prefer to use `flow.stateIn(..., WhileSubscribed(5_000), ...)` to stop subscribing after 5 seconds.
- Always use `DispatcherProvider` to inject Coroutine dispatchers.
- Use `TestDispatcherProvider` in unit tests, which maps all dispatcher to a single `TestCoroutineDispatcher`.

# Project: DeviceDx - Android (Jetpack Compose)

## Tech Stack
- Kotlin, Coroutines, Flow
- Jetpack Compose (UI)
- Architecture: MVVM with Clean Architecture
- Dependency Injection: Hilt
- Navigation: Compose Navigation 3 (accept tradeoff of using alpha)

## Compose Coding Standards
- **State Hoisting**: Always hoist state to the ViewModel or a stateless Composable wrapper.
- **Naming**: Composables that return Unit must be PascalCase; those that return values should be camelCase.
- **Performance**: Use `remember` and `derivedStateOf` for expensive calculations. Mark domain models as `@Stable` or `@Immutable`.
- **Modifiers**: Pass a `modifier: Modifier = Modifier` as the first optional parameter to all UI Composables.
- **Scaffold**: Use `Scaffold` for top-level screens to handle TopBar and PaddingValues correctly.

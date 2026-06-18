# Full-Featured Dev Build

This folder holds the local-development version of the plugin source. It is not compiled into the
normal plugin artifact.

Current dev-only helpers include:

- configurable unique-hole object IDs for local ID testing
- temporary swap-to-unique-hole testing on arbitrary game objects
- the same gold/yellow face recolor mask used by the shipped plugin
- object-definition parsing for building the client-side swap stand-in

## How To Run

Use the dedicated Gradle task:

```powershell
.\gradlew.bat runDev
```

The normal shipped plugin still runs with:

```powershell
.\gradlew.bat run
```

The `devTools` source set is separate from `src/main/java`, so these local helpers do not ship to
players and are not part of the Plugin Hub build.

# Minecraft versions above 1.21.11

This build supports the released versions **26.1, 26.1.1, 26.1.2, 26.2, and 26.3**.
Use the JAR whose `+mc<version>` suffix matches your Minecraft installation.
The original `src/` and root Gradle build still target 1.21.11.

## Requirements

- JDK / Java **25** for these Minecraft versions.
- Fabric Loader **0.19.5 or newer**.
- Fabric API and Cloth Config for your exact Minecraft version.
- Mod Menu for that version (optional, opens the settings screen).

| Minecraft | Fabric API | Mod Menu | Cloth Config (Fabric) |
| --- | --- | --- | --- |
| 26.1 / 26.1.1 / 26.1.2 | 0.155.3+26.1.2 | 18.0.2 | 26.1.154 |
| 26.2 | 0.161.0+26.2 | 20.0.3 | 26.2.155 |
| 26.3 | 0.165.2+26.3 | 21.0.0 | 26.3.159 |

Dependencies are installed separately in the game's `mods/` folder. Install just one version of this mod.

## Build

Run from the repository root on Windows:

```powershell
# Build every supported modern version:
.\build-modern.bat

# Build one version:
.\build-modern.bat 26.3

# Equivalent Gradle command (quote the property in PowerShell):
.\gradlew.bat -p ports "-Ptarget=26.3" build
```

The installable JAR appears in `ports/build/<version>/libs/`.
Do not install the `-sources.jar`.

On other systems with a Gradle 9.8 installation, use
`gradle -p ports -Ptarget=26.3 build`.

## Updating for another release

`common/java/` holds the shared code using Mojang's official names.
`versions/<family>.properties` pins the matching dependencies.
Small Java API differences live in `versions/<family>/java/`; a class here replaces
the common class with the same relative path for that release family.
The 26.1 hotfixes share the 26.1 profile but are compiled and packaged separately.

For a new release, add a dependency profile, add the version to the supported
lists in `build.gradle` and `../build-modern.bat`, port any changed APIs, and
compile and test in-game before claiming support. Do not broaden the
Minecraft dependency to include untested future versions.

The build uses the unobfuscated Fabric Loom plugin for 26.x; Yarn mappings
are only used by the original 1.21.11 build.

## Validation

Each listed target is checked with Gradle `build` (compilation, resource processing,
and JAR packaging). There are no automated gameplay tests. In-game checks should
cover sending and receiving pings between two clients, friend lists, settings, markers, dimension filtering, and sound.

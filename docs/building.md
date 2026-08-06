# Building and CI

## Java versions

The project targets Java 8 bytecode.

Gradle itself should run using a JDK compatible with the Gradle version used by the project. The wrapper contains JDK-discovery logic so an incompatible ambient Java installation does not accidentally start Gradle.

## Build

Linux/macOS:

```bash
./gradlew clean build
```

Windows:

```bat
gradlew.bat clean build
```

## Override Gradle JVM

Linux/macOS:

```bash
NILSDK_GRADLE_JAVA_HOME=/path/to/jdk-21 ./gradlew clean build
```

PowerShell:

```powershell
$env:NILSDK_GRADLE_JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
.\gradlew.bat clean build
```

The environment-variable name above is historical and should be renamed as part of the NilKit migration only when the corresponding launcher scripts are updated at the same time.

## Optional NilLoader decompile

```bash
./gradlew decompileNilloader
```

Normal builds should not decompile NilLoader as a side effect.

## GitHub Actions

The project currently has separate workflows for:

- normal build/test on commits and pull requests
- tagged releases

Cross-platform CI should cover Windows and Linux at minimum. macOS can be added if the project wants CI-backed claims for all three major desktop operating systems.

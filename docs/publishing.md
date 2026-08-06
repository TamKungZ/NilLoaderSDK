# Project-local Maven publishing

The `4.0.0` build can publish signed artifacts into a Maven repository inside the project rather than `~/.m2`.

> The names and coordinates below are the **historical NilKit 4.0.0 values**.
> They should be changed consistently when the NilKit API rename is applied to Gradle metadata.

## Publish

```bash
./gradlew publishProjectLocal
```

Windows:

```bat
gradlew.bat publishProjectLocal
```

Historical repository path:

```text
maven/me/tamkungz/nilkit/nilkit/4.0.0/
```

Historical classified artifacts:

```text
nilkit-4.0.0-all.jar
nilkit-4.0.0-mapping-tool.jar
```

The publication also includes the normal JAR, sources JAR, Javadoc JAR, POM/module metadata, checksums, and OpenPGP signatures.

## GPG

The build uses Gradle `useGpgCmd()` and therefore your normal GnuPG configuration and `gpg-agent`.

To choose a specific key, prefer your user Gradle configuration:

```properties
signing.gnupg.keyName=YOUR_KEY_ID
```

Do not commit private keys or passphrases to the repository.

## Consumer example (historical coordinates)

```gradle
repositories {
    maven { url = uri('/absolute/path/to/project/maven') }
}

dependencies {
    implementation 'me.tamkungz.nilkit:nilkit:4.0.0'
}
```

These coordinates should not be copied into new NilKit documentation after the rename is finalized.

# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)
- Coil 2.7.0 (authenticated image loads)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`). GitHub Actions signing is [build automation](build-automation.md).

Debug application id is `com.burton.photos.debug` so it can sit next to a signed install.

```bash
./scripts/install-git-hooks.sh
```

## Layout

```
app/src/main/java/com/burton/photos/
  MainActivity.kt              nav, picker, camera capture
  data/api/                    OkHttp client, Bearer interceptor
  data/parse/                  TinyJson + PhotosJson
  data/local/                  MediaStore camera roll
  data/prefs/                  DataStore session
  data/upload/                 JPEG convert + queue
  data/repository/             PhotosRepository
  domain/                      models
  ui/login, library, viewer, albums, browse, search, settings, local, theme
app/src/test/java/…            JSON, query, local ids, HEIC helper tests (no device)
```

Parser tests cover auth config, native login JSON (`token`), photo pages, albums, and query-string building. Run those before changing network parsing.

## Talking to a local API

The emulator’s host loopback is `http://10.0.2.2:8787` (the Photos API default). A physical phone needs the LAN IP. Do not point at the Vite port (`5174`).

The API must include native Bearer support (`X-Burton-Client: android`). See [burton-photos docs/android.md](https://github.com/Burton-Workspaces/burton-photos/blob/master/docs/android.md).

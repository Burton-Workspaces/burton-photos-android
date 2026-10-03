# Burton Photos

Android client for a self-hosted [Burton Photos](https://github.com/Burton-Workspaces/burton-photos) library — and a camera-roll gallery when you have no server yet. Browse local photos immediately, then connect an origin later. On a server: timeline, albums, favorite and archive, and **upload** from the gallery or camera.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-photos-android/releases). Droidify / F-Droid: [burton-sonos-fdroid](https://github.com/Burton-Workspaces/burton-sonos-fdroid) (`https://burton-workspaces.github.io/burton-sonos-fdroid/fdroid/repo`).

## What it does

- **Offline / on this phone** — first launch opens the camera roll; connect a server whenever you want
- **Library** — paged thumbnail grid; tap a photo for EXIF, favorite, and archive
- **Upload** — FAB opens the system picker; the camera button captures a JPEG; HEIC is converted on the phone (server session only)
- **Albums** — list, create, open (device folders while offline)
- **Search** — title, filename, place, camera (filename locally)
- **Favorites / Archive / folders / labels / people / moments / calendar** — the same grid with query filters (some need a server)
- **Settings** — connect or disconnect, who you are, API health, server origin

The phone talks to your Photos API with `Authorization: Bearer` and `X-Burton-Client: android`. Cookie login stays the browser default.

## Requirements

- Android 8.0+ (API 26)
- Optional: a reachable Burton Photos server (LAN HTTP or HTTPS)
- Photos API that accepts native Bearer sessions (see that repo’s [Android clients](https://github.com/Burton-Workspaces/burton-photos/blob/master/docs/android.md) doc)

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Screens, upload, permissions |
| [Architecture](docs/architecture.md) | Packages, Bearer, Coil, upload queue |
| [Development](docs/development.md) | Build, run, test, layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Self-hosted repo, Fingerprint, Pages publish script |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.photos.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

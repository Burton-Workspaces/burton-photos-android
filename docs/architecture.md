# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, OkHttp, and Coil. UI never talks HTTP directly; screens collect `PhotosRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Photo, Album, User, PhotoQuery, UploadJob, SessionState
data/
  api        OkHttp PhotosApi, AuthInterceptor, SessionStore
  parse      TinyJson + PhotosJson
  prefs      DataStore LocalPrefs
  upload     JpegConverter + sequential UploadQueue
  repository PhotosRepository
di/          OkHttp client + Coil ImageLoader (Application ImageLoaderFactory)
```

## Auth

The browser uses HTTP-only cookie `burton_session`. This app sends:

```
X-Burton-Client: android
Authorization: Bearer <session-token>
```

on every `/api` and `/media` request. Login, register, and `GET /api/auth/me` return `token` + `expiresAt` only when that client header is present, so a web XSS cannot read the session. Family mode still auto-issues a session; `/api/auth/me` is enough to persist Bearer.

`AuthInterceptor` reads `SessionStore`. `LocalPrefs` hydrates that store on process start.

## Media

Photo DTOs expose relative `thumbUrl` / `originalUrl`. `SessionStore.absolute` prefixes the configured origin. `BurtonPhotosApplication` implements Coil’s `ImageLoaderFactory` so the singleton loader uses the same OkHttp client (Bearer on thumbs). Do not load `/media` with an unauthenticated HTTP stack.

## Uploads

`UploadQueue` is a process singleton. The FAB and camera enqueue `content://` URIs. A mutex drains the queue: convert HEIC (and unknown types) to JPEG, `POST /api/photos/upload` multipart field **`file`**, then mark the job done. Library shows progress from `UploadQueue.jobs`.

Engine formats that skip conversion: jpeg, png, webp, gif, tiff.

## Library snapshot

`PhotosRepository.library` holds the current `PhotoQuery`, page of photos, and totals. Tab screens (library, favorites, archive, browse) call `refreshLibrary` with a different query. Offset paging uses `limit=120`.

## UI shell

Unsigned users see login/register only. After sign-in, `MainActivity` hosts a `NavHost` and a bottom bar: Library → Albums → Favorites → More. Upload FAB is on those tabs. Photo viewer, album detail, search, settings, and browse filters hide the bar (except search/settings/browse keep no FAB when not a bottom tab).

# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, OkHttp, and Coil. UI never talks HTTP directly; screens collect `PhotosRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Photo, Album, User, PhotoQuery, UploadJob, SessionState
data/
  api        OkHttp PhotosApi, AuthInterceptor, SessionStore
  local      MediaStore gallery (offline camera roll)
  edit       Color matrices, crop math, save edited JPEG to MediaStore
  parse      TinyJson + PhotosJson
  prefs      DataStore LocalPrefs
  upload     JpegConverter + sequential UploadQueue
  repository PhotosRepository
di/          OkHttp client + Coil ImageLoader (Application ImageLoaderFactory)
```

## Auth

First launch is **local** (`SessionState.Local`): no server, no login gate. Connect later from Library / More / Settings. Login and register are ordinary routes on the same `NavHost`.

After a server session exists, the browser uses HTTP-only cookie `burton_session`. This app sends:

```
X-Burton-Client: android
Authorization: Bearer <session-token>
```

on every `/api` and `/media` request. Login, register, and `GET /api/auth/me` return `token` + `expiresAt` only when that client header is present, so a web XSS cannot read the session. Family mode still auto-issues a session; `/api/auth/me` is enough to persist Bearer.

`AuthInterceptor` reads `SessionStore`. `LocalPrefs` hydrates that store on process start. Disconnect (`logout`) clears the token and returns to `SessionState.Local`; the origin stays so the connect screen can prefill it.

## Media

Photo DTOs expose relative `thumbUrl` / `originalUrl`. `SessionStore.absolute` prefixes the configured origin, and leaves `content://` / `file://` / `http(s)` URIs alone so the camera roll loads through Coil. `BurtonPhotosApplication` implements Coil’s `ImageLoaderFactory` so the singleton loader uses the same OkHttp client (Bearer on thumbs). Do not load `/media` with an unauthenticated HTTP stack.

Local photos are MediaStore rows mapped onto the same `Photo` model (`local-{id}`). Folders are buckets (`bucket-{id}`). Favorites use `IS_FAVORITE` on Android 11+. Photo and album nav arguments are `photoId` / `albumId` so a folder id is never parsed as a photo.

The editor loads the original (downsampled to 4096px), applies crop / rotate / flip plus a 4×5 color matrix, and inserts a new JPEG under `Pictures/Burton Photos`. It does not overwrite the source or upload the result.

## Uploads

`UploadQueue` is a process singleton. The FAB and camera enqueue `content://` URIs. A mutex drains the queue: convert HEIC (and unknown types) to JPEG, `POST /api/photos/upload` multipart field **`file`**, then mark the job done. Library shows progress from `UploadQueue.jobs`.

Engine formats that skip conversion: jpeg, png, webp, gif, tiff.

## Library snapshot

`PhotosRepository.library` holds the current `PhotoQuery`, page of photos, and totals. Tab screens (library, favorites, archive, browse) call `refreshLibrary` with a different query. Offset paging uses `limit=120`. In local mode the same snapshot is filled from MediaStore instead of `/api/photos`.

## UI shell

Everyone sees the main shell. Library is the start destination. Unsigned users browse the camera roll; **Connect** opens login/register. After sign-in, the bottom bar is Library → Albums → Favorites → More. Upload FAB is on those tabs only while a server session exists. Photo viewer, editor, album detail, search, settings, and browse filters hide the bar (except search/settings/browse keep no FAB when not a bottom tab).

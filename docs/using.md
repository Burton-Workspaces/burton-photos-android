# Using the app

Burton Photos browses photos on this phone by default. A self-hosted [Burton Photos](https://github.com/Burton-Workspaces/burton-photos) server is optional: originals then stay on that host, and the phone keeps the server URL and a session token.

## First launch

The library opens on your camera roll. Allow photo access when Android asks. You can use the app without a server.

To attach a library later, tap **Connect** on the Library header, **Connect to a server** in More or Settings, and enter the origin (`http://192.168.x.x:8787` or `https://photos.example`).

- **Family / one mode** (`autoLogin`): the library switches to that server without a password.
- **Accounts mode**: email and password. Registration is offered when the server allows it.

Seeded web logins (password `burton`) work the same here. The emulator’s host loopback is `http://10.0.2.2:8787`.

Tap **Not now** to stay on the phone gallery. **Disconnect server** in Settings returns to that gallery and keeps the last origin so reconnect is easy.

## Screens

### Library

Thumbnail grid of the timeline. Scroll loads the next page. Tap a cell for the viewer.

On a server, the **+** button opens the system photo picker (multiple images). The camera icon captures a photo through the device camera app. Uploads run one at a time; a bar at the top of Library shows the current file.

HEIC/HEIF from the gallery is converted to JPEG on the phone. The server’s media engine does not accept HEIC.

Without a server, Library is the camera roll. Search still filters by filename. The upload FAB is hidden.

### Viewer

Full image, title, EXIF, labels, and people. On a server, heart toggles favorite and archive / unarchive matches the web app (there is no hard delete). Local photos show filename and date only.

### Albums

On a server: cover grid, type a title, **Create**, open an album.

Without a server: device folders (Camera, Screenshots, …). Creating albums needs a server.

### Favorites

On a server, the same grid filtered to `favorite=true`. On this phone (Android 11+), the tab lists photos marked favorite in the system gallery.

### More

Folders and calendar work locally. Labels, people, moments, and archive need a server. Settings is always available; **Connect to a server** is at the top while you are offline.

### Search

Type two or more characters. Against a server this is the same photo query as the web header (`q`). Locally it matches filenames.

### Settings

**On this phone:** connect CTA and app version.

**Signed in:** name and email, auth mode, server origin, API health checks, app version. **Disconnect server** forgets the session and returns to the camera roll.

## Permissions

| Permission | Why |
| --- | --- |
| Photos / Images | Browse the camera roll in offline mode (Android 13+ `READ_MEDIA_IMAGES`; older `READ_EXTERNAL_STORAGE`) |
| Selected photos | Android 14+ can grant only the pictures you pick; the grid shows that subset |
| Internet | Talk to the Photos API and load thumbs after you connect a server |
| Photo picker | No extra storage permission for uploads on Android 13+; the system picker grants one-shot access |
| Camera app | Capture uses `FileProvider` in app cache; the camera app writes the JPEG |

Cleartext HTTP is allowed so a LAN install without Caddy still works. HTTPS with Caddy’s local CA needs that CA installed on the phone.

## What lives on the phone

Server URL, session token, and last known user in DataStore (`burton_photos`). Thumbnails are Coil’s HTTP cache. Capture files sit in `cache/captures/` until uploaded. Offline mode reads the system MediaStore; it does not copy your camera roll into the app.

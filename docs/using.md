# Using the app

Burton Photos is a client of **your** Photos server. Originals stay on that host. The phone keeps the server URL and a session token on-device.

## First launch

Enter the library origin (`http://192.168.x.x:8787` or `https://photos.example`) and tap **Use this server**.

- **Family / one mode** (`autoLogin`): the library opens without a password.
- **Accounts mode**: email and password. Registration is offered when the server allows it.

Seeded web logins (password `burton`) work the same here.

## Screens

### Library

Thumbnail grid of the timeline. Scroll loads the next page. Tap a cell for the viewer.

The **+** button opens the system photo picker (multiple images). The camera icon captures a photo through the device camera app. Uploads run one at a time; a bar at the top of Library shows the current file.

HEIC/HEIF from the gallery is converted to JPEG on the phone. The server’s media engine does not accept HEIC.

### Viewer

Full image, title, EXIF, labels, and people. Heart toggles favorite. Archive / unarchive matches the web app (there is no hard delete).

### Albums

Cover grid. Type a title and **Create**. Open an album to see its photos.

### Favorites

Same grid filtered to `favorite=true`.

### More

Folders, labels, people, moments, calendar, archive, and settings. Each browse row opens a filtered library.

### Search

Type two or more characters. The same photo query as the web header (`q`).

### Settings

Signed-in name and email, auth mode, server origin, API health checks, app version. **Sign out** is disabled when the server is in family mode.

## Permissions

| Permission | Why |
| --- | --- |
| Internet | Talk to the Photos API and load thumbs |
| Photo picker | No storage permission on Android 13+; the system picker grants one-shot access |
| Camera app | Capture uses `FileProvider` in app cache; the camera app writes the JPEG |

Cleartext HTTP is allowed so a LAN install without Caddy still works. HTTPS with Caddy’s local CA needs that CA installed on the phone.

## What lives on the phone

Server URL, session token, and last known user in DataStore (`burton_photos`). Thumbnails are Coil’s HTTP cache. Capture files sit in `cache/captures/` until uploaded.

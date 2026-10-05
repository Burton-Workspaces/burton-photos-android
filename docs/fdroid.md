# F-Droid / Droidify repository

GitHub Releases are APK downloads. [Droidify](https://github.com/Droid-ify/client) and the official F-Droid client do **not** subscribe to those. They need an **F-Droid repository**: a public HTTPS folder with signed APKs plus a signed catalog (`index-v1.jar`).

This is a **self-hosted simple binary repo** of the same APKs CI already signs. It is not submission to [f-droid.org](https://f-droid.org/), which rebuilds from source and signs with F-Droid’s key.

Official HOWTO: [Setup an F-Droid App Repo](https://f-droid.org/docs/Setup_an_F-Droid_App_Repo/).

Burton Android apps share one Pages catalog: [Burton-Workspaces/burton-app-dist](https://github.com/Burton-Workspaces/burton-app-dist). Publish this APK into that same `repo/` so Droidify users already subscribed keep seeing updates.

## Two keys

Do not mix these up.

| Key | File | What it signs | Used where |
| --- | --- | --- | --- |
| **App signing key** | `release.jks` (alias `burton`) | The APK | Android update verification. Same key as [build-automation.md](build-automation.md) / GitHub Releases. |
| **Repo signing key** | Created by `fdroid init` (`keystore.jks` in the fdroid working directory) | The repo index (`index-v1.jar`) | Droidify / F-Droid **Fingerprint** |

The Fingerprint is the **SHA-256 of the repo certificate**, 64 hex characters with no colons. Clients pin it so a fake catalog at the same URL cannot replace yours. It is **not** the APK keystore fingerprint.

Keep the repo keystore and `config.yml` private and backed up. Never publish `config.yml` (it contains passwords). Never commit either keystore to git.

Rotating the **repo** key means every user must re-add the repository. Rotating `release.jks` is worse: Android will refuse APK updates.

Current catalog Fingerprint (from [burton-app-dist/FINGERPRINT](https://github.com/Burton-Workspaces/burton-app-dist/blob/main/FINGERPRINT)):

`D517D045B3E2FB297C0EC0BBA17AFF03488A4BB4EF431331A3A1C3FB46A5EFB6`

## Create the repo

Do this on a machine that is **not** the public web server (laptop is fine). If you already publish Burton Sonos / Meeting / Slack / Weather, reuse that `~/fdroid` working tree.

`./scripts/setup-fdroid-and-secrets.sh` installs `fdroidserver` if needed, reuses or creates `~/fdroid`, clones the Pages repo, and then writes this app’s GitHub signing secrets.

```bash
pipx install fdroidserver
export PATH="$HOME/.local/bin:$PATH"
which fdroid   # must be $HOME/.local/bin/fdroid, not /usr/bin/fdroid
mkdir -p ~/fdroid && cd ~/fdroid
fdroid init
chmod 0600 config.yml
```

Do **not** use Debian’s `apt install fdroidserver` (2.2.1). That stack’s Androguard cannot scan APKs from Android Gradle Plugin 8.7 (`res1 must be zero!` / `resources.arsc`). `./scripts/publish-fdroid-pages.sh` prepends `~/.local/bin` and refuses `/usr/bin/fdroid` for that reason.

Set `repo_url` to:

`https://burton-workspaces.github.io/burton-app-dist/fdroid/repo`

Clone [burton-app-dist](https://github.com/Burton-Workspaces/burton-app-dist) **next to** this app repo (`../rabun-app-dist`). Pages is served from `main` at `/`. The `/fdroid/repo` path is filled by the publish script.

Seed metadata lives in [`fdroid/metadata/com.burton.photos.yml`](../fdroid/metadata/com.burton.photos.yml). The setup/publish scripts copy it into `$FDROID_ROOT/metadata/` if missing.

## Publish with the Pages script

`scripts/publish-fdroid-pages.sh` copies a signed APK into your private `fdroid` working tree, runs `fdroid update`, then mirrors **only** `repo/` into [burton-app-dist](https://github.com/Burton-Workspaces/burton-app-dist) and pushes.

It will not use this Android repo as the Pages target, and it will not copy `config.yml` or the repo keystore. `FDROID_PAGES_DIR` defaults to `../rabun-app-dist` when that clone exists.

```bash
export FDROID_ROOT=~/fdroid
cp fdroid-pages.env.example fdroid-pages.env   # optional; source it if you want
./scripts/publish-fdroid-pages.sh 0.1.0
```

`FDROID_ASSEMBLE=1` forces `assembleRelease`. `FDROID_PAGES_PUSH=0` commits without pushing.

Add the catalog in Droidify:

`https://burton-workspaces.github.io/burton-app-dist/fdroid/repo?fingerprint=D517D045B3E2FB297C0EC0BBA17AFF03488A4BB4EF431331A3A1C3FB46A5EFB6`

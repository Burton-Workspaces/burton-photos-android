# Build automation setup

This repo already contains the GitHub Actions workflows. Setup is **repository settings + signing secrets**, not new YAML.

After this is done:

- Every push/PR to `master` runs unit tests and Conventional Commit checks
- Conventional `feat` / `fix` commits on `master` open a release-please PR
- Merging that PR tags `vX.Y.Z`, creates a GitHub Release, and uploads `burton-photos-<version>.apk`

Day-to-day versioning is in [releases.md](releases.md). Commit message rules are in [CONTRIBUTING.md](../CONTRIBUTING.md).

## What is already in git

| Path | Role |
| --- | --- |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | `testDebugUnitTest` on PR and `master` |
| [`.github/workflows/conventional-commits.yml`](../.github/workflows/conventional-commits.yml) | Commit subjects and PR titles |
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | release-please; packs APK when a release is created |
| [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml) | Signed `assembleRelease`, upload APK |
| [`.github/actions/setup-android-ci`](../.github/actions/setup-android-ci/action.yml) | Temurin 17, Android SDK `platform-tools`, `local.properties` |
| [`release-please-config.json`](../release-please-config.json) | SemVer, `CHANGELOG.md`, tags `vX.Y.Z` |
| [`scripts/install-git-hooks.sh`](../scripts/install-git-hooks.sh) | Local `commit-msg` hook |

Release-please only runs when `github.repository` is `Burton-Workspaces/burton-photos-android`. Forks still get CI tests.

## 1. Enable Actions

**Settings → Actions → General**

- Allow Actions (default actions + reusable workflows is enough)
- **Workflow permissions:** Read and write
- Check **Allow GitHub Actions to create and approve pull requests**

Without that checkbox, release-please can push `release-please--branches--master` but fails with *GitHub Actions is not permitted to create or approve pull requests*.

`GITHUB_TOKEN` is enough. Do not put a personal access token in the workflows for this.

## 2. Local keystore (machine)

Gitignore already excludes `keystore.properties`, `*.jks`, and `*.keystore`.

```bash
cp keystore.properties.example keystore.properties
```

Create a JKS if you do not have one (alias `burton` matches CI’s default):

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias burton \
  -keyalg RSA -keysize 2048 -validity 10000
```

Fill `keystore.properties`:

```
storeFile=release.jks
storePassword=…
keyAlias=burton
keyPassword=…
```

`storeFile` is a path relative to the repo root. Confirm a local signed build:

```bash
./gradlew assembleRelease
```

That writes `app/build/outputs/apk/release/app-release.apk`. Never commit the JKS or `keystore.properties`.

## 3. GitHub secrets (CI signing)

There is no env var named `KEYSTORE_BASE64` on your machine. CI reconstructs `release.jks` and `keystore.properties` from secrets:

| GitHub secret | Local source |
| --- | --- |
| `KEYSTORE_BASE64` (**required**) | Base64 of the **file** named in `storeFile` (the JKS bytes) |
| `KEYSTORE_PASSWORD` (**required**) | `storePassword` |
| `KEY_ALIAS` | `keyAlias` (optional; default `burton`) |
| `KEY_PASSWORD` | `keyPassword` (optional; default store password) |

One-shot (also sets up F-Droid working tree):

```bash
./scripts/setup-fdroid-and-secrets.sh
```

Or set secrets by hand:

```bash
gh secret set KEYSTORE_BASE64 --body "$(base64 -w0 release.jks)"
gh secret set KEYSTORE_PASSWORD
```

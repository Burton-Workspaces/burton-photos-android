# Releases

How versions are cut once automation is already configured. **Local signed build + GitHub Release + F-Droid Pages:** [Local release build and publish](#local-release-build-and-publish) below. **First-time GitHub Actions, permissions, and signing secrets:** [build-automation.md](build-automation.md).

Versioning is **SemVer**. The Gradle `versionName` and `versionCode` both come from [`version.txt`](../version.txt):

```
versionCode = MAJOR * 1_000_000 + MINOR * 1_000 + PATCH
```

Tags look like `v0.1.0` (`include-v-in-tag` in `release-please-config.json`).

## Conventional Commits

Merges to `master` **must** use [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). CI rejects other subjects (and pull request titles). Install the local hook with `./scripts/install-git-hooks.sh`. Details: [CONTRIBUTING.md](../CONTRIBUTING.md).

| Prefix | Effect |
| --- | --- |
| `feat:` | minor bump (pre-1.0 also uses minor for features; `bump-minor-pre-major` is on) |
| `fix:` | patch |
| `feat!:` / `BREAKING CHANGE:` | major |
| `chore:`, `docs:`, `ci:` | no version bump unless configured otherwise |

The release PR updates `version.txt`, `CHANGELOG.md`, and `.release-please-manifest.json`. Merging it tags `vX.Y.Z` and creates the GitHub Release.

The **Release** workflow uses `GITHUB_TOKEN`. The repository must allow Actions to open PRs:

**Settings → Actions → General → Workflow permissions**
- Read and write permissions
- **Allow GitHub Actions to create and approve pull requests**

The APK pack still runs from that workflow when a release is created, and from tag pushes (`release-assets.yml` uploads with `--clobber`).

## CI

| Workflow | When | What |
| --- | --- | --- |
| [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) | PR and push to `master` | `testDebugUnitTest` |
| [`.github/workflows/conventional-commits.yml`](../.github/workflows/conventional-commits.yml) | PR (including title edits) and push to `master` | Conventional Commit subjects |
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | push to `master` | release-please; if a release was created, pack APK |
| [`.github/workflows/release-assets.yml`](../.github/workflows/release-assets.yml) | tag `v*.*.*`, workflow_call, or `workflow_dispatch` | test, signed `assembleRelease`, upload `burton-photos-<version>.apk` |

The tag must match `version.txt` (without the `v`). Duplicate uploads use `--clobber`.

## Signing

Local and CI signing is documented in [build-automation.md](build-automation.md). Never commit `keystore.properties` or the keystore.

## Local release build and publish

The version argument **must match** [`version.txt`](../version.txt). Both scripts accept `0.1.0` or `v0.1.0`.

Do **not** hand-edit `version.txt` to invent a new number. After a `feat:` / `fix:` on `master`, merge the release-please PR so `version.txt` and tag `vX.Y.Z` move together. The **Release-please** job only runs when `github.repository` is `Burton-Workspaces/burton-photos-android`.

### One-time setup

```bash
./scripts/setup-fdroid-and-secrets.sh
```

That script is idempotent. It reuses `~/fdroid` and `../burton-sonos-fdroid` when they already exist, copies a sibling Burton JKS if this repo has no keystore yet, creates `Burton-Workspaces/burton-photos-android` if needed, and writes `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD`.

### Publish a version

```bash
export FDROID_ROOT=~/fdroid
./scripts/upload-release-apk.sh 0.1.0
./scripts/publish-fdroid-pages.sh 0.1.0
```

`upload-release-apk.sh` assembles the signed APK and attaches `burton-photos-0.1.0.apk` to the GitHub Release. `publish-fdroid-pages.sh` copies that APK into the shared Pages catalog. Details: [fdroid.md](fdroid.md).

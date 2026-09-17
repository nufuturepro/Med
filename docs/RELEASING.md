# Releasing Med RX

Releases are built by GitHub Actions whenever a tag `v*` is pushed. The
workflow signs both APKs (phone + Wear) with the release keystore stored in
repo secrets and publishes them to GitHub Releases — the exact feed the
in-app updater polls (`nufuturepro/MedRX`).

## One-time setup: upload signing secrets

The keystore lives **outside the repo** (never commit it). From the repo
root (`Med/`), with the keystore at `../medrx-keystore/medrx-release.jks`:

```bash
# 1. Base64-encode the keystore (Git Bash / WSL / macOS / Linux)
base64 -w0 ../medrx-keystore/medrx-release.jks > ks.b64

# 2. Upload it as the KEY_STORE_FILE secret (needs GitHub CLI)
gh secret set KEY_STORE_FILE < ks.b64
rm ks.b64

# 3. Upload the credentials — take the password from
#    ../medrx-keystore/CREDENTIALS.txt (never paste it in chat/issues)
gh secret set KEY_STORE_PASSWORD
gh secret set ALIAS          # value: medrx
gh secret set KEY_PASSWORD   # same password as KEY_STORE_PASSWORD
```

No GitHub CLI? Repo → Settings → Secrets and variables → Actions →
"New repository secret", paste the base64 text for `KEY_STORE_FILE`.

> When uploading base64 by hand, make sure the editor doesn't wrap lines —
> the CI decodes the secret exactly as stored.

## Cutting a release

1. Bump `versionCode` (+1, never reuse) and `versionName` in **both**
   `app/build.gradle.kts` and `wear/build.gradle.kts` (keep them in sync).
2. Commit to `main` and push.
3. Tag and push the tag:

   ```bash
   git tag v2.1.0-fork.1 && git push origin v2.1.0-fork.1
   ```

4. Watch the Actions tab; when green, the Release appears with
   `MedRX-…-phone.apk` and `MedRX-…-wear.apk`.
5. Sanity-check: the updater compares `versionName` strings, so a new
   release must have a strictly greater version than the previous one.

## Signing-key rules

- **One key forever.** Same package (`com.nukirk.medrx`) + new key = update
  refused on every user device. Losing this key ends update-in-place for
  Med RX; keep `medrx-keystore/` backed up somewhere safe (password
  manager + offline copy).
- Never commit `*.jks`, `*.keystore`, or `keystore.properties` (gitignored —
  verify with `git status` after creating them locally).
- For local signed builds, create `keystore.properties` in the repo root
  (gitignored):

  ```properties
  KEY_STORE_FILE=../medrx-keystore/medrx-release.jks
  KEY_STORE_PASSWORD=...
  KEY_ALIAS=medrx
  KEY_PASSWORD=...
  ```

  then `./gradlew :app:assembleRelease :wear:assembleRelease`.

## Signing certificate fingerprint

```
SHA-256: 6A:CE:BC:23:0B:15:C4:E6:16:11:A5:AC:57:37:EE:A5:09:90:88:F6:8C:13:72:FA:65:61:4A:BF:3B:A6:3A:AD
```

Publish this in the README so users can verify downloaded APKs
(`keytool -printcert -jarfile MedRX-2.1.0-fork.1-phone.apk`).

## Migrating users from the original Med

The package id differs, so both apps install side by side. Data moves via
the app's own tools: original Med → Settings → export backup (JSON) and/or
CSV → Med RX → import. The reader ignores unknown keys, so exports from
either app load cleanly.

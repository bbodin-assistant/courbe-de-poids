# Android release signing

The GitHub Actions workflow builds the distributable APK and AAB as signed Release artifacts.

## GitHub Actions secrets

Create these four **Repository secrets** under:

**Settings → Secrets and variables → Actions → New repository secret**

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64 encoding of the release `.jks` keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Alias of the signing key |
| `ANDROID_KEY_PASSWORD` | Password of the signing key |

Never commit the `.jks` file, passwords, or a decoded keystore to Git.

## Create the signing key once

Run locally on a secure machine:

```bash
keytool -genkeypair -v \
  -keystore courbe-de-poids-release.jks \
  -alias courbe-de-poids \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype JKS
```

Use a strong password for the keystore and another strong password for the key when prompted.

The package/application ID is:

```
fr.bbodin.courbedepoids
```

Keep the resulting `courbe-de-poids-release.jks` and both passwords backed up securely. **Do not regenerate this key for future releases**: all updates must keep the same signing identity.

## Convert the keystore to the GitHub secret

Linux/macOS:

```bash
base64 -w 0 courbe-de-poids-release.jks
```

On macOS, if `-w` is unavailable:

```bash
base64 courbe-de-poids-release.jks | tr -d '\n'
```

Copy the complete output into the `ANDROID_KEYSTORE_BASE64` GitHub secret.

Then create the other three secrets with the exact values used during key generation.

## Result

After the four secrets exist, push to `main`. The workflow will:

1. restore the keystore only on the GitHub runner;
2. build `app-release.apk`;
3. build `app-release.aab`;
4. verify the APK signature with `apksigner`;
5. publish both signed artifacts.

The Android instrumentation tests remain on the debug build because they are test artifacts, not distributable packages.

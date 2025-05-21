# Publishing to Maven Central

This project is set up to publish to Maven Central via Sonatype OSSRH.

## Automatic Publishing with GitHub Actions

This project uses GitHub Actions to automatically publish to Maven Central:

1. **Snapshot Releases**:
   - Every push to the `master` branch publishes a snapshot release
   - The version from `build.sbt` is used (should end with `-SNAPSHOT`)

2. **Official Releases**:
   - Create and push a tag with format `v1.2.3` to trigger a release
   - The version is automatically extracted from the tag name
   - The workflow publishes the artifacts to Maven Central and releases them

## Required GitHub Secrets

The following secrets must be set in the GitHub repository:

- `SONATYPE_USERNAME`: Your Sonatype OSSRH username
- `SONATYPE_PASSWORD`: Your Sonatype OSSRH password
- `PGP_SECRET`: Your GPG private key exported as base64
  ```
  gpg --export-secret-keys YOUR_KEY_ID | base64 -w0
  ```
- `PGP_PASSPHRASE`: The passphrase for your GPG key

## Manual Publishing

### Prerequisites

1. You must have a Sonatype OSSRH account linked to the `yarkivaev` group ID
2. Set up your credentials in `~/.sbt/1.0/sonatype.sbt`:

```scala
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "s01.oss.sonatype.org",
  "your-sonatype-username",
  "your-sonatype-password"
)
```

3. Make sure you have GPG installed and a key pair generated:
   - Generate a key: `gpg --gen-key`
   - List keys: `gpg --list-keys`
   - Distribute your public key: `gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID`

### Publishing Process

1. **Prepare the release**:
   - Update version in `build.sbt` (remove `-SNAPSHOT` suffix)
   - Build and test your project: `sbt clean test`

2. **Publish to Sonatype staging**:
   ```
   sbt publishSigned
   ```

3. **Release to Maven Central**:
   ```
   sbt sonatypeBundleRelease
   ```

### Snapshot Releases

For snapshot releases (versions ending in `-SNAPSHOT`):

```
sbt publishSigned
```

Snapshots are automatically published without requiring manual release.

## Troubleshooting

- If you get GPG errors, try:
  ```
  echo "pinentry-mode loopback" >> ~/.gnupg/gpg.conf
  ```

- Check Sonatype staging repositories at: https://s01.oss.sonatype.org/#stagingRepositories
# Release Guide – `release/8.6` (DEPRECATED)

> [!WARNING]
> **The 8.6 release line is deprecated.** `release/8.6` no longer gets regular patch releases.
> Releases from this branch are for **emergencies only**, e.g. a critical security fix that customers on 8.6 cannot wait
> to receive by upgrading. For every other change, target `main` or a supported release branch.
> For releasing supported lines, follow the [release guide on `main`](https://github.com/camunda/sap-connectors/blob/main/RELEASE.md).

Nothing in this branch blocks an emergency release: the release workflow still works as before. Every release run from this
branch is flagged as deprecated (run name, input description, a warning annotation and the job summary), and its GitHub
releases are never marked as "Latest".

## What gets released

A single release run publishes **both** connectors at the same version `8.6.<patch>`:

| Connector | Artifact | Destination |
|-----------|----------|-------------|
| OData | Docker image `camunda/sap-odata-connector:X.Y.Z` | Docker Hub |
| OData | Element templates JSON + `mtad.yaml.example` | GitHub Release (`odata-X.Y.Z`) |
| RFC | `.war` file + element templates JSON + `mtad.yaml.example` | GitHub Release (`rfc-X.Y.Z`) |

## Emergency release

### 1. Make sure it really is an emergency

- The fix addresses a critical issue (typically a security vulnerability) that must be shipped for 8.6.
- Upgrading to a supported version is not an acceptable workaround for the affected customers.
- The team agrees on releasing from the deprecated line.

Merge the fix into `release/8.6` through a regular PR (or a backport PR) and make sure the `build-and-test` workflow is green.

### 2. Pick the patch version

The `release/8.6` workflow does **not** validate the patch number against `pom.xml`, and `pom.xml` on this branch may lag behind
the last release. Pick the next unused patch number from the existing tags:

```bash
git ls-remote --tags origin 'refs/tags/rfc-8.6.*' 'refs/tags/odata-8.6.*'
```

E.g. if the highest tag is `rfc-8.6.5`, release patch `6`. Do not reuse a patch number that already has a tag.

### 3. Trigger the release workflow

Go to **Actions → "Release all artifacts"** (https://github.com/camunda/sap-connectors/actions/workflows/build-and-publish.yml), click **"Run workflow"**, select the branch `release/8.6` and enter the patch number
only (e.g. `6`, not `8.6.6`).

### 4. Expect the deprecation warnings

The run is named `[DEPRECATED 8.6] …` and shows a "Deprecated release line 8.6" warning for each connector. They are informational
only and do not fail the run.

### 5. Verify the release

- [ ] GitHub releases exist for `odata-X.Y.Z` and `rfc-X.Y.Z` with the correct artifacts, and neither is marked as "Latest".
- [ ] `camunda/sap-odata-connector:X.Y.Z` is visible on Docker Hub.

> **Docker `latest` tag:** the OData release workflow does not push `camunda/sap-odata-connector:latest`, so an emergency 8.6
> release does not move `latest` away from the newest supported version.

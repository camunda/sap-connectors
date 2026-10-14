# Release Guide – `release/8.7` (DEPRECATED)

> [!WARNING]
> **The 8.7 release line is deprecated.** `release/8.7` no longer gets regular patch releases.
> Releases from this branch are for **emergencies only**, e.g. a critical security fix that customers on 8.7 cannot wait
> to receive by upgrading. For every other change, target `main` or a supported release branch.
> For releasing supported lines, follow the [release guide on `main`](https://github.com/camunda/sap-connectors/blob/main/RELEASE.md).

Nothing in this branch blocks an emergency release: the release workflow still works as before. Every release run from this
branch is flagged as deprecated (run name, input description, a warning annotation and the job summary), and its GitHub
releases are never marked as "Latest".

## What gets released

A single release run publishes **both** connectors at the same version `8.7.<patch>`:

| Connector | Artifact | Destination |
|-----------|----------|-------------|
| OData | Docker image `camunda/sap-odata-connector:X.Y.Z` | Docker Hub |
| OData | Element templates JSON + `mtad.yaml.example` | GitHub Release (`odata-X.Y.Z`) |
| RFC | `.war` file + element templates JSON + `mtad.yaml.example` | GitHub Release (`rfc-X.Y.Z`) |
| RFC | `scratch` image containing only the released WAR (for CVE scanning, not a supported runtime) | Internal GAR `europe-west1-docker.pkg.dev/team-infosec/camunda/sap-rfc-connector:X.Y.Z` |

## Emergency release

### 1. Make sure it really is an emergency

- The fix addresses a critical issue (typically a security vulnerability) that must be shipped for 8.7.
- Upgrading to a supported version is not an acceptable workaround for the affected customers.
- The team agrees on releasing from the deprecated line.

Merge the fix into `release/8.7` through a regular PR (or a backport PR) and make sure the `build-and-test` workflow is green.

### 2. Pick the patch version

`pom.xml` on `release/8.7` carries the next version to release as `8.7.<patch>-SNAPSHOT` (e.g. `8.7.6-SNAPSHOT`).
The patch input must match that number: the workflow fails if it does not, or if a release tag for that version already exists.

### 3. Trigger the release workflow

Go to **Actions → "Release all artifacts"** (https://github.com/camunda/sap-connectors/actions/workflows/build-and-publish.yml), click **"Run workflow"**, select the branch `release/8.7` and enter the patch number
only (e.g. `6`, not `8.7.6`).

### 4. Expect the deprecation warnings

The run is named `[DEPRECATED 8.7] …` and shows a "Deprecated release line 8.7" warning for each connector. They are informational
only and do not fail the run.

### 5. Verify the release

- [ ] GitHub releases exist for `odata-X.Y.Z` and `rfc-X.Y.Z` with the correct artifacts, and neither is marked as "Latest".
- [ ] `camunda/sap-odata-connector:X.Y.Z` is visible on Docker Hub.
- [ ] `europe-west1-docker.pkg.dev/team-infosec/camunda/sap-rfc-connector:X.Y.Z` is visible in the internal Artifact Registry.
- [ ] Merge the "prepare next development version" PR the workflow opened against `release/8.7`.

If the run failed after part of the release was already published, use "Re-run failed jobs" on that run instead of starting a new one.

> **Docker `latest` tag:** the OData release workflow does not push `camunda/sap-odata-connector:latest`, so an emergency 8.7
> release does not move `latest` away from the newest supported version.

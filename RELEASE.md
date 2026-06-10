# Release Guide

This document explains how to cut a new release of the Camunda SAP Connectors.

## Version scheme

Versions follow `MAJOR.MINOR.PATCH`, where:

- `MAJOR.MINOR` is locked to the Camunda product release (e.g. `8.9` for Camunda 8.9).
- `PATCH` is incremented for each connector release within that minor line.

Example: `sap-rfc-connector-8.9.3` is the third release of the RFC connector for Camunda 8.9.

Each `MAJOR.MINOR` line has its own long-lived branch: `release/8.9`, `release/8.8`, etc.

## What gets released

A single release run publishes **both** connectors at the same version:

| Connector | Artifact | Destination |
|-----------|----------|-------------|
| OData | Docker image `camunda/sap-odata-connector:X.Y.Z` | Docker Hub |
| OData | Element templates JSON + `mtad.yaml.example` | GitHub Release (`odata-X.Y.Z`) |
| RFC | `.war` file + element templates JSON + `mtad.yaml.example` | GitHub Release (`rfc-X.Y.Z`) |

## Prerequisites

1. **You are on the right release branch.** The workflow refuses to run on `main` or feature branches — it must be `release/X.Y` (e.g. `release/8.9`).
2. **The patch version has been bumped in a PR.** Before triggering a release, open a PR against the target release branch that increments the `<version>` in the root `pom.xml` (patch only — never touch major or minor). Merge it before proceeding.
3. **CI is green.** Confirm that the latest commit on the release branch passes the `build-and-test` workflow.

## Step-by-step release

### 1. Bump the patch version (if not done yet)

Open a PR against `release/X.Y` with the new version in `pom.xml`:

```xml
<!-- change this -->
<version>8.9.3-SNAPSHOT</version>
<!-- to this -->
<version>8.9.4-SNAPSHOT</version>
```

Merge the PR and wait for CI to go green.

### 2. Trigger the release workflow

Go to **Actions → "Release all artifacts"** in the GitHub UI, or use the direct link:

```
https://github.com/camunda/sap-connectors/actions/workflows/build-and-publish.yml
```

Click **"Run workflow"** and fill in the two fields:

| Field | Value | Example |
|-------|-------|---------|
| **Branch** | The release branch to cut from | `release/8.9` |
| **patch-version** | The patch number only (not the full version) | `4` |

The final version is assembled automatically: the workflow reads `MAJOR.MINOR` from the `pom.xml` on the selected branch and appends the patch number you provided → `8.9.4`.

### 3. What the workflow does

```
validate-branch
    └── build-final-version          (reads MAJOR.MINOR from pom.xml, appends PATCH)
            ├── odata-release        (tests → GitHub release → Docker push)
            └── rfc-release          (tests → build .war → GitHub release)
```

Both connector releases run in parallel after the version is resolved.

**OData pipeline:**
1. Runs build + tests (including CAP bookshop smoke tests).
2. Creates a GitHub release tagged `odata-X.Y.Z` with element templates and `mtad.yaml.example`.
3. Builds the Docker image with `mvn versions:set` and pushes `camunda/sap-odata-connector:X.Y.Z` to Docker Hub.

**RFC pipeline:**
1. Runs unit tests and smoke tests.
2. Sets the version, builds the `.war`.
3. Creates a GitHub release tagged `rfc-X.Y.Z` with the `.war`, element templates, and `mtad.yaml.example`.

### 4. Verify the release

After the workflow succeeds:

- [ ] GitHub releases exist for `odata-X.Y.Z` and `rfc-X.Y.Z` with the correct artifacts.
- [ ] `camunda/sap-odata-connector:X.Y.Z` is visible on Docker Hub.
- [ ] Download and sanity-check the element templates JSON from each release.

## Releasing multiple minor lines

When you need to release across several supported branches (e.g. backporting a fix to 8.7, 8.8, and 8.9), run the workflow once per branch.

> **Release from oldest to newest.** The `latest` Docker tag is driven by whichever push lands last. To ensure `latest` always points to the newest version, release `release/8.7` first, then `release/8.8`, then `release/8.9`.

## Creating a new release branch

When a new Camunda minor version ships (e.g. Camunda 8.10):

1. Create `release/8.10` from `main`.
2. Open a PR to bump `<version>` in `pom.xml` to `8.10.0-SNAPSHOT` and update `version.camunda` to the matching Camunda BOM version.
3. Merge and verify CI is green on the new branch before cutting the first release.

## Troubleshooting

**Workflow fails with "can only be run on branches with 'release/' prefix"**  
You triggered the workflow on `main` or a feature branch. Re-run it and select a `release/X.Y` branch.

**Version mismatch**  
The patch-version input is just the number (e.g. `4`), not the full string `8.9.4`. Double-check the input field.

**OData tests fail due to missing CAP bookshop image**  
The reusable test workflow builds the CAP bookshop image as part of CI — this should not happen in the workflow, but if tests fail locally, run:
```bash
docker build -t camunda/sap-odata-connector/cap-bookshop odata-connector/cap-bookshop
```

**RFC JCo classes not found (local build only)**  
The JCo dependency is excluded for licensing reasons. For a local build only, install it manually:
```bash
mvn install:install-file -Dfile=sapjco3.jar \
  -DgroupId=com.sap.conn.jco \
  -DartifactId=com.sap.conn.jco.sapjco3 \
  -Dversion=3.1.10 \
  -Dpackaging=jar
```
Then uncomment the `sapjco` section in `rfc-connector/pom.xml`. This is not needed for the CI release workflow.

**Code formatting check fails**  
Run `mvn spotless:apply` to auto-format, then commit and push.

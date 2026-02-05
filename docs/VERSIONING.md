# NerveMind Versioning Strategy

NerveMind follows [Semantic Versioning 2.0.0](https://semver.org/).

## Versioning Scheme

We maintain two distinct versions to ensure stability for our plugin ecosystem:

1.  **Core Application Version** (`appVersion`): The version of the main desktop application.
2.  **Plugin API Version** (`pluginApiVersion`): The version of the `ai.nervemind:plugin-api` library.

Both versions are defined in the `gradle.properties` file in the project root.

### Core Application Version
Format: `MAJOR.MINOR.PATCH`

-   **MAJOR**: Significant breaking changes or architectural shifts.
-   **MINOR**: New features (backward compatible).
-   **PATCH**: Bug fixes.

### Plugin API Version
Format: `MAJOR.MINOR.PATCH`

This version is **decoupled** from the App version. It only changes when the `plugin-api` module changes.

-   **MAJOR**: Breaking changes to Plugin Interfaces. **All plugins must be recompiled/updated.**
-   **MINOR**: New interfaces or methods added (backward compatible).
-   **PATCH**: Javadoc fixes or internal optimizations (no interface changes).

## Single Source of Truth

The `gradle.properties` file is the master record for versions.

```properties
appVersion=1.0.0-SNAPSHOT
pluginApiVersion=1.0.0-SNAPSHOT
```

-   **Development Builds**: Always end with `-SNAPSHOT`.
-   **Releases**: Remove `-SNAPSHOT` for the release commit, then bump to next `-SNAPSHOT`.

## Plugin Compatibility

Plugins must declare their compatible API version range in `plugin.json`.

```json
{
  "id": "com.example.myplugin",
  "minApiVersion": "1.0.0",
  "maxApiVersion": "2.0.0"
}
```

This ensures that a plugin built for API v1.0.0 continues to work with future App versions as long as the App supports API v1.x.
## Release Process

We use an automated pipeline to build and publish installers.

### 1. Prepare the Release
- Ensure `appVersion` in `gradle.properties` is correct (e.g., `0.1.0`).
- Update the `CHANGELOG.md` (if applicable).
- Commit and push your changes to `main`.

### 2. Trigger the Build
Push a version tag to GitHub. Tags must start with `v` followed by the version (e.g., `v0.1.0`).

```bash
# Example: Creating and pushing a tag
git tag v0.1.0
git push origin v0.1.0
```

### 3. Automated CD Pipeline
Once the tag is pushed, the **Installer build** workflow starts:
- **Builds**: Windows (MSI & Portable), macOS (Intel & ARM), and Ubuntu (DEB).
- **Bundles**: A custom GraalVM runtime (no Java required by users).
- **Releases**: Creates a **Draft Release** on GitHub with all installers attached.

### 4. Finalize
Go to the **Releases** section on GitHub, review the draft, and click **Publish**.

---

> [!TIP]
> **CI vs CD**: Routine commits to `main` only trigger fast builds and tests to ensure code quality. Full installers are only built on Tags or manual trigger.

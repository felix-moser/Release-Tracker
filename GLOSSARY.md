# Release Tracking

Release Tracker monitors tracked repositories and tracked Docker images for new releases and creates Jira tickets using ticket templates.

## Language

**Check cycle**:
One run that checks the tracked repositories and tracked Docker images for updates and reports the results.
_Avoid_: Scan, sweep

**Manual check**:
A user-requested check cycle. If a cycle is already active, the request shares that cycle and its results.
_Avoid_: Forced refresh

**Scheduled check**:
A check cycle requested automatically at the configured interval.
_Avoid_: Scheduler tick

**Tracked Docker image**:
A container image monitored for manifest changes on the `latest` tag. Any tag provided when adding the image is stripped. A tracked Docker image is either a **standalone Docker image** or a **linked Docker image**.
_Avoid_: Container repo, Docker container

**Standalone Docker image**:
A tracked Docker image that is not associated with any tracked GitHub repository and is managed in its own section of the UI.

**Linked Docker image**:
A tracked Docker image configured directly within a tracked GitHub repository's settings (at most one per repository). When both the repository's GitHub release and its linked Docker image change in the same check cycle, only the GitHub release ticket is created.

**Manifest digest**:
The content-addressable `sha256:...` identifier of the `latest` image manifest returned by a container registry for a tracked Docker image.
_Avoid_: Image hash, manifest JSON, layer digest

**Short digest**:
The abbreviated hexadecimal prefix (first 12 hex characters after `sha256:`) of a manifest digest, exposed to Jira ticket templates through the `{digest}` placeholder.
_Avoid_: Full digest

**Registry credential**:
A stored authentication configuration (registry host, username, and password or token) used to authenticate with a container registry when checking private images.
_Avoid_: Docker login, registry secret

**Fetched release**:
The latest release observed from GitHub for a tracked repository, whether or not its required ticket work has succeeded.
_Avoid_: Processed release

**Processed release**:
A release whose required ticket work has succeeded and which Release Tracker has recorded as handled.
_Avoid_: Fetched release, latest GitHub release

**Release link**:
The GitHub page for a tracked repository's latest release, included in a Jira ticket through the `{url}` placeholder.

**Version comparison mode**:
A tracked repository's rule for deciding whether a fetched release qualifies for processing relative to its recorded release.

**Any different version**:
The default version comparison mode, in which any difference between the fetched and recorded release tags qualifies for processing.

**Higher version only**:
A version comparison mode that qualifies numerically higher versions for processing, falling back to Any different version when either tag cannot be compared numerically. Numeric comparison ignores one leading `v` or `V`, compares dot-separated numeric components from left to right, and treats missing components as zero.

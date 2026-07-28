# Refactor — `/v1` API versioning & `DetectionService` consolidation

> Refactoring milestone documented by the Git tag **`refactor/v1-api-and-detection-service`**.
> This document explains *why* and *how* the changes were made, so other developers can pick up the
> code without re-reading every diff.

## Scope

Four commits, applied in this order on `preprod` (the graph order is authoritative, not the commit
dates):

| Order | Commit    | Subject |
|-------|-----------|---------|
| 1 | `bc616216` | `chore: refactor api using /v1 path and set as default` |
| 2 | `dec31f9e` | `chore: refactor ZoneService into DetectionService only` |
| 3 | `da060bfe` | `chore: move non controller outside versioning` |
| 4 | `e572f043` | `chore: exclude rest DTO detection from detectionService` |

These are all refactors: **no change to the public API contract**. The historical paths are still
served identically (see "Backward compatibility").

---

## 1. `bc616216` — Introduce `/v1` versioning (v1 as default)

### Goal
Prepare the API to host multiple versions side by side while keeping the existing URLs working,
without breaking any client.

### What changes
- **New meta-annotation `@V1RestController`**
  (`endpoint/rest/V1RestController.java`). It combines `@RestController` +
  `@RequestMapping({"", "/v1"})`. Every handler is therefore exposed **both** on the bare path
  (`/image`) **and** under the explicit prefix (`/v1/image`). V1 is thus the "default" version.
- **Controllers grouped under the `controller/v1/` package** (and their mappers under
  `controller/v1/mapper/`). Controllers switch from `@RestController` to `@V1RestController`.
- **`DetectionController` split**: the "zones" part is extracted into a new
  `controller/v1/ZoneDetectionController.java`.
- **The `readme` package leaves `endpoint.rest`**: `endpoint.rest.readme.*` →
  `app.bpartners.geojobs.readme.*` (monitor + webhook). Readme monitoring/webhook is a
  cross-cutting concern, not a versioned REST endpoint.
- **OpenAPI specs frozen per version** under `doc/api/v1/v1-x-y-api.yml`.
- `SecurityConf`: Readme imports updated; the `requestMatchers` still use the bare paths — the
  `/v1` prefix is allowed separately (see commits `5d56013c` "add /v1 endpoint as allowed path"
  and the `V1PathAccessIT` test that verifies the dual exposure).

### Things to watch going forward
- To add a v1 endpoint: annotate the controller with `@V1RestController` and place it under
  `controller/v1/`. It is automatically served on both `/…` and `/v1/…`.
- To introduce a **v2**: create a `@V2RestController` (`@RequestMapping("/v2")`) and a
  `controller/v2/` package. Do **not** add `""` to its mapping, otherwise it collides with v1 on
  the bare path.

---

## 2. `dec31f9e` — Merge `ZoneService` → `DetectionService`

### Goal
Remove an artificial service boundary: `ZoneService` and `DetectionService` operated on the same
domain. One service, one entry point.

### What changes
- All of `ZoneService`'s behavior is moved into `DetectionService`; `ZoneService.java` is
  **deleted**.
- Callers (e.g. `DetectionAddressConversionJobStatusChangedService`) and tests
  (`ZoneServiceIT` → `DetectionServiceIT`, `ZoneServiceTest` → `DetectionServiceTest`) are rewired
  onto `DetectionService`.

### Things to watch
- `DetectionService` becomes large; that is accepted for this milestone. Any later extraction
  should be done by responsibility (e.g. export, addresses), not by "zone vs detection".

---

## 3. `da060bfe` — Move non-versioned controllers out of `/v1`

### Goal
Some endpoints must **not** be versioned with the business API: they are infrastructure (captcha
verification, Readme webhook). `bc616216` had moved them into `/v1` as part of the sweep; this
commit fixes that.

### What changes
- `CaptchaVerificatorController` and `ReadmeController`: `controller/v1/` → `controller/`
  (back to a plain, unprefixed `@RestController`, so no `/v1` variant).

### Rule to remember
- **Business** endpoint exposed to clients → `@V1RestController` in `controller/v1/`.
- **Infra / webhook / callback** endpoint → plain `@RestController` in `controller/`, outside
  versioning.

---

## 4. `e572f043` — `DetectionService` no longer returns REST DTOs

### Goal
Decouple the service layer from the REST layer. Previously `DetectionService` returned
`endpoint.rest.model.Detection` (generated DTOs) directly. The service now works in the **domain
model**; the DTO translation is pushed down to the controller.

### What changes
- `DetectionService` methods return the domain `repository.model.detection.Detection` instead of
  the REST DTO.
- **New `controller/v1/mapper/DetectionRestMapper`**: `toRest(Detection)` /
  `toRest(List<Detection>)`. The v1 `DetectionController` wraps every service return value in
  `detectionRestMapper.toRest(...)`.
- **Support for "steps computed on the fly"** (tiling / machine detection, not persisted):
  - `Detection.computedStep`: a `@Transient` field, plus `Detection.getCurrentStep()` which
    returns the persisted step if present, otherwise the computed step.
  - `DetectionStep.statistic`: a `@Transient` `TaskStatistic` (excluded from `equals`/`toString`),
    carrying the detailed statistics of a step computed at request time.
  - `DetectionRestMapper.toRest` relies on `getCurrentStep()` + `statistic` to pick the right
    mapper (`DetectionFromStatisticRestMapper`), falling back to `REQUEST_ACCEPTED` / an empty
    statistic.

### Things to watch
- **Do not** reintroduce any `endpoint.rest.model.*` type into `DetectionService` signatures.
  The domain → DTO conversion belongs to the controller (via `DetectionRestMapper`).
- The `computedStep` / `statistic` fields are **transient**: they only live for the duration of a
  request and must never be persisted.

---

## Backward compatibility

- All historical paths (`/image`, `/detections/*`, …) are still served identically **and** are now
  also reachable under `/v1/…`.
- Infra endpoints (captcha, Readme webhook) stay on their bare path, without `/v1`.
- The public OpenAPI contract is unchanged; the versioned specs live under `doc/api/v1/`.

## Key files to know

- `endpoint/rest/V1RestController.java` — versioning annotation (dual exposure).
- `endpoint/rest/controller/v1/` — versioned v1 controllers + mappers.
- `endpoint/rest/controller/v1/ZoneDetectionController.java` — zones (extracted from DetectionController).
- `endpoint/rest/controller/v1/mapper/DetectionRestMapper.java` — domain → REST DTO boundary.
- `service/DetectionService.java` — single service (former ZoneService merged in), returns domain.
- `readme/` — Readme monitoring & webhook, outside `endpoint.rest`, not versioned.
- `doc/api/v1/` — OpenAPI specs frozen per version.

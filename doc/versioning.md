# OpenAPI (OAS3) Versioning Policy

This document explains **how the version number of our OpenAPI 3 specification evolves**.
It is the reference for anyone who edits [`doc/public-api.yml`](public-api.yml) /
[`doc/api.yml`](api.yml) and needs to decide *which* part of the version number to bump.

The specification follows [Semantic Versioning 2.0.0](https://semver.org): `MAJOR.MINOR.PATCH`
(e.g. `1.3.5`). The number lives in the `info.version` field of the spec and in the version list
under `info.description`.

> **One rule of thumb:** *any* change to the spec file bumps *at least* the PATCH number.
> A change is never merged without moving the version forward.

---

## The three parts of the version

| Part      | Bump it when…                                                     | Breaks existing clients? |
|-----------|-------------------------------------------------------------------|--------------------------|
| **PATCH** | The change is **editorial / cosmetic** — it does not alter the contract | No  |
| **MINOR** | You **add** something new to the public API (backward-compatible) | No  |
| **MAJOR** | You make a **breaking change** to the public API                  | Yes |

When you bump a higher part, the lower parts reset to `0`
(`1.3.5` → minor → `1.4.0`; `1.4.0` → major → `2.0.0`).

---

## PATCH — editorial changes (`x.y.Z`)

Bump the **PATCH** number for any modification that changes the *text* of the spec but **not the
contract**. If a client generated from the old spec still works exactly the same against the new
one, it is a patch.

Typical patch-level changes:

- Fixing a typo, rewording or clarifying a `description` or `summary`.
- Adding, removing or reformatting a comma, a character, a line break, or indentation.
- Reordering fields, tags, or example values with no semantic effect.
- Adding or improving `example` values.
- Fixing an internal `$ref` that resolves to the same schema.

**Example — `1.3.2` → `1.3.3`** rewrote the `GET /image` summary and expanded its `description`:

```yaml
# before (1.3.2)
summary: Retrieve aerial image from a textual, human-readable, address

# after (1.3.3)
summary: Retrieve an aerial image from a human-readable address or geographic coordinates.
description: |
  Retrieve an aerial image by providing either a human-readable address
  or a pair of geographic coordinates (longitude and latitude).
```

> Rule of thumb: *"I only changed words, punctuation, or formatting."* → **PATCH**.

---

## MINOR — additive changes (`x.Y.0`)

Bump the **MINOR** number when you **add a new capability** to the public API **without breaking**
any existing client. Old clients keep working; new clients gain something.

Typical minor-level changes:

- Adding a **new endpoint** (path / operation).
- Adding a **new optional attribute** to a request body or a schema.
- Adding a **new field** to a response payload.
- Adding a **new optional query / header parameter**.
- Adding a **new value** to an enum (when clients tolerate unknown values).
- Adding a **new schema / component** used by new endpoints.

The key word is **optional / additive**: nothing that a current client sends or expects may change.

**Example — new optional attributes added to a request schema:**

```yaml
complexityFactor:
  type: number
  description: |
    Number between 0 and 1. Actual default value is `0.66` when none provided.
    Permits consumer to define complexity factor in lidar processing.
knn:
  type: integer
```

Because these fields are new and optional, they do not affect existing callers → **MINOR** bump.

> Rule of thumb: *"I added something new, and every old request still behaves the same."* → **MINOR**.

---

## MAJOR — breaking changes (`X.0.0`)

Bump the **MAJOR** number when a change **can break an existing client**. A consumer generated from
the previous spec may now fail to compile, send invalid requests, or misread responses.

Typical major-level (breaking) changes:

- **Removing** or **renaming** an endpoint, field, parameter, or enum value.
- Making a previously **optional** parameter or field **required**.
- **Changing the type** of a field (e.g. `string` → `integer`), its format, or its units.
- **Tightening validation** (new `pattern`, smaller `maxLength`, narrower range) that rejects
  previously valid input.
- Changing the **default behavior** or the **meaning** of an existing field.
- Changing an HTTP **status code** or the **shape** of an existing response.
- Removing or changing the **authentication** scheme.

> Breaking changes should be rare and deliberate. Prefer an additive MINOR change (e.g. a new field
> alongside the old one) over a breaking one whenever possible.

> Rule of thumb: *"Could a client that worked yesterday break today?"* → **MAJOR**.

---

## Decision flow

```
Did the change alter the API contract (paths, schemas, params, responses, auth)?
│
├─ No  → only wording / punctuation / formatting / examples ................. PATCH  (x.y.Z)
│
└─ Yes
   │
   ├─ Is it purely additive & backward-compatible?
   │      (new optional field, new endpoint, new response field, new enum value)  MINOR  (x.Y.0)
   │
   └─ Could it break an existing client?
          (removal, rename, required-now, type change, stricter validation) ...  MAJOR  (X.0.0)
```

---

## Release checklist — cutting a new version

When you change the spec, do all of the following in the same change set:

1. **Bump `info.version`** in [`doc/public-api.yml`](public-api.yml) according to the rules above.
2. **Freeze the new version**: copy the spec to `doc/version/vX-Y-Z-api.yml` (see
   [`doc/version/`](version/)). This preserves an immutable snapshot each published version can be
   compared against.
3. **Update the version list** in `info.description` of both `public-api.yml` and `api.yml`:
   add the new line `- [vX.Y.Z (YYYY-MM-DD)](…/doc/version/vX-Y-Z-api.yml)`, move the `*(current)*`
   marker to it, and mark any retired version `*(deprecated)*` when relevant.
4. Keep `doc/api.yml` (`version: 'latest'`, internal spec) and `doc/public-api.yml` (public spec)
   consistent for any change that touches public endpoints.

> `doc/api.yml` is the **internal / local-development** spec (`version: latest`); `doc/public-api.yml`
> is the **public** spec that carries the real SemVer number. See [`doc/README.md`](README.md).

---

## Version history

Snapshots live under [`doc/version/`](version/). Current published history:

| Version  | Date       | Kind of change            | Status       |
|----------|------------|---------------------------|--------------|
| v1.3.5   | 2026-05-12 | additive (lidar options)  | **current**  |
| v1.3.4   | 2026-03-31 | additive                  |              |
| v1.3.3   | 2026-03-11 | additive + editorial      |              |
| v1.3.2   | 2026-02-25 | editorial / additive      |              |
| v1.3.1   | 2026-02-20 | patch                     |              |
| v1.3.0   | 2026-02-16 | minor                     |              |
| v1.2.0   | 2025-12-10 | minor                     |              |
| v1.1.0   | 2025-05-13 | minor                     |              |
| v1.0.0   | 2024-10-25 | initial release           | deprecated   |

> Note: version numbers only ever move **forward**. Never re-publish a different spec under an
> already-released version number — cut a new PATCH instead.

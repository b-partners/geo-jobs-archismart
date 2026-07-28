# API Documentation

This project includes **two OpenAPI specifications** to serve different purposes:

- **Local Development Specification**: Designed for internal use within the development team. It contains additional
  details specific to our local setup, such as mock services or environment-specific configurations.

- **External API Specification**: Intended for external consumers of our API. This version only includes the public
  endpoints

## Versioning

The OpenAPI specification follows Semantic Versioning (`MAJOR.MINOR.PATCH`). See
[versioning.md](versioning.md) for the full policy: when to bump PATCH (editorial changes), MINOR
(additive changes), or MAJOR (breaking changes), and the release checklist. Per-version snapshots
are kept under [version/](version/).

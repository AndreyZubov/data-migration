# Salesforce Data Migration

A modular Java toolkit for migrating data into and between Salesforce orgs.

> **Status:** early development — Step 1 (domain core + API contract) implemented.

## Overview

The project is a multi-module Maven build targeting Java 21. It is being built up incrementally; the modules currently present are:

| Module | Purpose |
|--------|---------|
| `core` | Pure-Java domain model, mapping engine, format converters (JSON / XML / CSV / XLSX), schema validation. No Spring, no JPA. |
| `api`  | OpenAPI specification (`openapi.yaml`) plus generated DTOs and server interfaces. |

Future modules (`bulk-client`, `web`, `batch`, `web-ui`) will be introduced in subsequent steps.

## Requirements

- JDK 21 (LTS)
- Maven 3.9+

## Build

```bash
mvn -pl core,api verify
```

This compiles all modules, runs unit tests, generates DTOs from `api/src/main/resources/openapi.yaml`, and enforces a minimum line coverage of 80% on `core`.

## Project layout

```
.
├── core/                 # Domain, mapping, format converters, validation
│   ├── src/main/java/io/datamigration/core/
│   │   ├── domain/       # Records: MigrationJob, FieldMapping, Org, ...
│   │   ├── mapping/      # Mapper + Transform implementations
│   │   ├── format/       # RecordReader / RecordWriter per format
│   │   └── validation/   # SchemaValidator, TargetSchema, FieldDefinition
│   └── src/test/java/    # JUnit 5 + ArchUnit
├── api/                  # OpenAPI contract + generated stubs
│   └── src/main/resources/openapi.yaml
└── pom.xml               # Parent
```

## Code style

The build uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format (AOSP style, 4-space indent). Apply formatting locally with:

```bash
mvn spotless:apply
```

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).

# Changelog

All notable changes to this project will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and [Semantic Versioning](https://semver.org/).

---

## Version 1.0.0

- Initial release of the Gradle plugin equivalent of
  [naming-convention-liquibase-maven-plugin](https://github.com/htshame/naming-convention-liquibase-maven-plugin).
- Depends on `io.github.htshame:ncl-core:4.0`.
- Triggered during the `check` lifecycle phase.
- Supports XML, YAML/YML, and JSON Liquibase changeLog formats.

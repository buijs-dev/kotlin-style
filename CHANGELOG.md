# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `KotlinStyle`: ktlint's `ktlint_official` code style with custom rules, max line length 100, up to 10 format runs
  and an optional license header.
- Rule set `buijs`:
  - `buijs:parameter-annotation-per-line`: every annotation and parameter on its own line in a multi-line parameter
    list.
  - `buijs:blank-line-between-annotated-parameters`: a blank line between annotated parameters in a multi-line primary
    constructor.
  - `buijs:single-line-lambda`: a lambda with one single-line expression stays on one line when it fits.
  - `buijs:no-empty-line-comment`: removes empty `//` comments.
  - `buijs:blank-line-after-closing-brace`: a blank line after a statement ending in `}`.
  - `buijs:single-line-condition`: `if` and `while` conditions on one line.
  - `buijs:license-header`: every `.kt` file opens with the configured license header.
- Line break preservation (`preserveLineBreaks`), on by default.
- A ktlint rule set (`META-INF/services`) for the ktlint CLI and IntelliJ plugin.
- The Gradle plugin `dev.buijs.kotlin-style`, with the tasks `kotlinStyleApply` (runs on build) and `kotlinStyleCheck`,
  executed in an isolated worker process.
- The CLI `kstyle`, with the commands `check` and `format`.
- Publishing with the vanniktech maven-publish plugin: releases and snapshots to Repsy
  (`https://repo.repsy.io/buijs-dev/maven2`); the `kstyle` jar on the GitHub Release.

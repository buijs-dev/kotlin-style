# Kotlin Style
[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/kotlin-style?color=black)](https://github.com/buijs-dev/kotlin-style/blob/main/LICENSE)
[![codecov](https://codecov.io/gh/buijs-dev/kotlin-style/graph/badge.svg?token=z2bkIG555a)](https://codecov.io/gh/buijs-dev/kotlin-style)

The Kotlin style: ktlint's `ktlint_official` code style with a little extra.

| Rule                                                 | What it does                                                                                                                    |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `buijs:parameter-annotation-per-line`           | In a multi-line parameter list, every annotation and the parameter itself on a line of its own.                                 |
| `buijs:blank-line-between-annotated-parameters` | In a multi-line primary constructor, one blank line between parameters when either is annotated.                                |
| `buijs:single-line-lambda`                      | A lambda holding one single-line expression stays on one line when it fits, so a wrapped call chain reads one call per line.    |
| `buijs:no-empty-line-comment`                   | Removes empty `//` comments, the old workaround to stop ktfmt from joining lines.                                               |
| `buijs:blank-line-after-closing-brace`          | A statement ending in `}` - a block, a lambda however short - is followed by a blank line before the next statement.            |
| `buijs:single-line-condition`                   | The condition of an `if`/`while` sits on one line. One that doesn't fit is reported for a hand to fix: extract it into a `val`. |
| `buijs:license-header`                          | With a license header configured, every `.kt` file opens with it (added by formatting, reported by checking); `.kts` files are left without. |

Four standard rules are off, each undoing or undone by a custom rule: `no-blank-line-in-list`,
`condition-wrapping`, and `kdoc` and `no-consecutive-comments` (both reject the `/** license */` header every file opens with). Max line length is 100; a function named in backticks (a test) doesn't count against it.

Formatting keeps every line break and blank line someone wrote; it only adds them (wrapping a
line that's too long, the blank lines above). Six more standard rules are off for that:
`function-expression-body`, `no-empty-first-line-in-method-block`,
`no-empty-first-line-in-class-body`, `no-blank-line-before-rbrace`,
`no-blank-lines-in-chained-method-calls` and `blank-line-between-when-conditions`, and
`single-line-lambda` only joins a lambda the formatter broke open itself. `preserveLineBreaks` off
(as for generated code) normalizes them instead.

ktlint's public API gives up after 3 format runs; deeply nested code (generated code in
particular) needs more, so `KotlinStyle` formats with up to 10.

```kotlin
@PersistenceEntity(MatchColumns.TABLE)
internal data class Match(
    @SearchSort(tiebreaker = true)
    @SearchFilter
    @PersistenceColumn(MatchColumns.PUBLIC_ID)
    val id: MatchId,

    @SearchFilter
    @PersistenceJoinOne(MatchColumns.GAME_ID)
    val game: GameRef,
)

fun top(x: List<Int>) =
    x
        .filter { it > 1 }
        .map { it * 2 }
        .sortedDescending()
        .take(10)
        .joinToString(separator = ", ")
```

## Modules
- `kotlin-style-rules` (`dev.buijs.kotlinstyle:kotlin-style-rules` on Maven Central) - the rules and
  `KotlinStyle`, the one entry point the CLI, the Gradle plugin and code generators share. Also a
  ktlint rule set (`META-INF/services`) for the ktlint CLI and IDE plugins.
- `kotlin-style-gradle-plugin` (`dev.buijs.kotlinstyle:kotlin-style-gradle-plugin` on Maven
  Central) - the plugin `dev.buijs.kotlin-style`.
- `kotlin-style-cli` - `kstyle`, attached to every
  [GitHub Release](https://github.com/buijs-dev/kotlin-style/releases) as `kstyle-<version>.jar`.

## Gradle
The plugin is on Maven Central, not on the Gradle Plugin Portal; in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

In `build.gradle.kts`:

```kotlin
plugins {
    id("dev.buijs.kotlin-style") version "1.0.0"
}

kotlinStyle {
    maxLineLength.set(100) // the default
    applyOnBuild.set(true) // the default
    preserveLineBreaks.set(true) // the default
    licenseHeader.set(file("LICENSE_HEADER.txt").readText()) // off by default
}
```

- `./gradlew kotlinStyleApply` formats every Kotlin source set and the `*.gradle.kts` scripts,
  then fails on what is left; what only a hand can fix (a condition too long for one line, a
  wildcard import, a file not named after its class). `build` runs it before compiling.
- `./gradlew kotlinStyleCheck` changes nothing and fails on anything not in style, for CI.

With `licenseHeader` set, every `.kt` file opens with that header: `kotlinStyleApply` adds it
where it is missing, `kotlinStyleCheck` reports it (`buijs:license-header`). Build scripts
are left without.

Anything under the build directory, generated code included, is left alone. The rules run in an
isolated worker process: ktlint brings its own Kotlin compiler, which never reaches the build
classpath or the Gradle daemon.

## Command line
The Gradle plugin and the rule set on Maven Central are the main ways to use the style. For
anything else, such as a pre-commit hook, every [GitHub Release](https://github.com/buijs-dev/kotlin-style/releases) has the
command line `kstyle` as one executable jar (Java 25), `kstyle-<version>.jar`, with its SHA-256 in
`kstyle-<version>.jar.sha256`:

```shell
VERSION=1.0.0
BASE=https://github.com/buijs-dev/kotlin-style/releases/download/v$VERSION
curl -fLO "$BASE/kstyle-$VERSION.jar"
curl -fLO "$BASE/kstyle-$VERSION.jar.sha256"
sha256sum -c "kstyle-$VERSION.jar.sha256"      # macOS: shasum -a 256 -c

java -jar kstyle-$VERSION.jar check src        # exit 0 in style, 1 on violations
java -jar kstyle-$VERSION.jar format src       # exit 0; what is left shows with check
java -jar kstyle-$VERSION.jar check --max-line-length 120 src
java -jar kstyle-$VERSION.jar format --no-preserve-line-breaks src
java -jar kstyle-$VERSION.jar format --license-header HEADER.txt src
```

Every `.kt` and `.kts` file under the given files or directories is checked or formatted;
`build/` and `.gradle/` directories are skipped. The defaults are those of the Gradle plugin;
a wrong command line prints the usage and exits with 2. From a checkout, `./gradlew :kotlin-style-cli:fatJar`
writes the same jar to `kotlin-style-cli/build/libs/kstyle.jar`.

## ktlint CLI and IntelliJ
The `kotlin-style-rules` jar is a ktlint rule set: add it to the ktlint CLI (`--ruleset`) or the
ktlint IntelliJ plugin as an external rule set. The jar only brings the `buijs:*` rules; the
settings of `KotlinStyle` go in `.editorconfig`:

```editorconfig
[*.{kt,kts}]
ktlint_code_style = ktlint_official
max_line_length = 100
ktlint_ignore_back_ticked_identifier = true
ktlint_standard_no-blank-line-in-list = disabled
ktlint_standard_condition-wrapping = disabled
ktlint_standard_kdoc = disabled
ktlint_standard_no-consecutive-comments = disabled
```

ktlint then formats as `KotlinStyle` with `preserveLineBreaks` off: line breaks and blank lines
someone wrote are not kept, and it formats with ktlint's own 3 runs. The license header is only
in the Gradle plugin and the CLI.

## Generated code
A code generator can format its output with the same style through `KotlinStyle` in
`kotlin-style-rules`, so generated and authored code look alike. Generated code has no line breaks
worth keeping:

```kotlin
val style = KotlinStyle(preserveLineBreaks = false)
val formatted = style.format(source, "Match.kt")
```

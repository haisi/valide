# valide

[![CI](https://github.com/haisi/valide/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/haisi/valide/actions/workflows/ci.yml)
[![Coverage Status](https://coveralls.io/repos/github/haisi/valide/badge.svg?branch=main)](https://coveralls.io/github/haisi/valide?branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/li.selman/valide.svg)](https://central.sonatype.com/artifact/li.selman/valide)
[![Javadoc](https://javadoc.io/badge2/li.selman/valide/javadoc.svg)](https://javadoc.io/doc/li.selman/valide)
[![License](https://img.shields.io/github/license/haisi/valide)](LICENSE)
[![Mutation Score](https://haisi.github.io/valide/pit/badge.svg)](https://haisi.github.io/valide/pit/)

Jakarta Bean Validation constraint that recursively validates that every attribute of a class or record is non-null unless annotated with JSpecify's @Nullable.

[**Website**](https://haisi.github.io/valide/)

## Usage

Add the dependency (a Bean Validation provider such as Hibernate Validator is assumed to be on the classpath
already):

```xml
<dependency>
    <groupId>li.selman</groupId>
    <artifactId>valide</artifactId>
    <version>VERSION</version>
</dependency>
```

Annotate a class or record with `@NullSafe`, and mark the parts that really may be null with JSpecify's
`@Nullable`:

```java
@NullSafe
record Order(
        String id,                              // must not be null
        @Nullable String note,                  // may be null
        List<String> tags,                      // list and every element must not be null
        List<@Nullable String> optionalTags,    // list must not be null, elements may be
        @Nullable List<String> extras,          // list may be null, its elements may not
        String[] codes,                         // array and every element must not be null
        String @Nullable [] altCodes,           // array may be null, its elements may not
        Map<String, @Nullable Address> shipTo,  // keys must not be null, values may be
        Customer customer) {}                   // must not be null, and is validated recursively
```

Because `@Nullable` is a `TYPE_USE` annotation it says precisely *which* part of a declaration may be null, and
`@NullSafe` honours that distinction — note how `@Nullable List<String>` and `List<@Nullable String>` mean
different things, as do `@Nullable String[]` and `String @Nullable []`.

Validation is recursive. Every non-null attribute is walked in turn, descending through arrays, `Iterable`s,
`Map`s and `Optional`s into the objects they hold. Nested types do **not** need to be annotated themselves —
unlike `@Valid`, the walk descends into them regardless. Shared and cyclic references are visited once, so a
parent/child back-reference does not loop.

Each offending attribute yields its own `ConstraintViolation`, with a property path locating it from the
annotated root:

```java
Set<ConstraintViolation<Order>> violations = validator.validate(order);
// order.customer.address.street  -> must not be null
// order.tags[2]                  -> must not be null
```

### What is left unchecked

Nullness is only asserted where the declaration states it, so `@NullSafe` never guesses:

- **Type variables and wildcards** — `T value`, `List<?> items`.
- **Containers whose element type cannot be read off the declaration** — a raw `List`, or a subtype that
  re-binds its supertype's parameters such as `interface Index<V> extends Map<String, V>`. Type arguments are
  read from the declaration itself and are not resolved through the supertype hierarchy, so
  `class Tags extends ArrayList<String>` carries no element type information either.
- **Types the walk treats as opaque leaves** — anything in `java.*`, `javax.*`, `jakarta.*`, `jdk.*`, `sun.*`
  or `com.sun.*`, plus enum constants and lambdas. Their *own* nullness is still checked; only their internals
  are not descended into.
- **Static and synthetic fields**, including the outer-instance reference of an inner class.

`@NullSafe` deliberately ignores JSpecify's `@NullMarked`/`@NullUnmarked` scoping: the rule is flat, so it
works the same on code that never adopted them.

Container positions are rendered into the path segment they belong to (`lines[2].sku`) rather than as separate
iterable nodes, so `getPropertyPath().toString()` reads as expected while `Path.Node#getKind()` reports
`PROPERTY` throughout.

## Building

```shell
./mvnw verify
```

Test coverage is enforced at 100% (line and branch) via JaCoCo; `verify` fails if it drops below that. Run
`open target/site/jacoco/index.html` after a build to see the report.

`verify` also runs Spotless (palantir-java-format + sorted `pom.xml`), Checkstyle, and Error Prone/NullAway via
the compiler plugin. Run `./mvnw spotless:apply` to auto-format before committing.

## Mutation Testing

[![Mutation Score](https://haisi.github.io/valide/pit/badge.svg)](https://haisi.github.io/valide/pit/)

Line/branch coverage only proves a test executed some code, not that it would notice a bug in it. [PIT
mutation testing](https://pitest.org) seeds small deliberate bugs ("mutants") into the compiled classes and
checks whether the test suite actually fails for each one; a mutant that survives is a gap in the tests.

Mutation testing runs nightly at around 02:00 UTC via `.github/workflows/pit-mutation-testing.yml`, and only
when at least one new commit has landed on `main` since the last successful run - so it stays off the critical
path for every push/PR while still picking up changes automatically. It can also be triggered manually from
the Actions tab.

See the full HTML mutation report at `https://haisi.github.io/valide/pit/` for a per-class,
per-mutator breakdown, or run it locally with:

```shell
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
open target/pit-reports/index.html
```

## Releasing

Releases are published to Maven Central via [JReleaser](https://jreleaser.org). Pushing a tag matching `v*`
(e.g. `v1.0.0`) triggers `.github/workflows/release.yml`, which stages the build artifacts and hands them to
JReleaser to sign and deploy to the [Central Portal](https://central.sonatype.com).

```shell
./bumpPomVersion.sh
git push
./release.sh
```

## Contributing

Bug reports, feature requests and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). This
project follows a [Code of Conduct](CODE_OF_CONDUCT.md); by participating you agree to abide by it.

## License

`valide` is licensed under the [Apache License, Version 2.0](LICENSE).

See `jreleaser.yml` for the deployment configuration.

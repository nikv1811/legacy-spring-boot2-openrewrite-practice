# Legacy Spring Boot 2 OpenRewrite Practice

An intentionally legacy Spring Boot 2.7 / Java 11 application to use as the **source** side of an OpenRewrite migration capstone.

## Legacy patterns included

- `WebSecurityConfigurerAdapter` with the older `authorizeRequests()` / `antMatchers()` DSL
- `RestTemplate`
- Spring Data JPA and `javax.persistence`
- Bean Validation and `javax.validation`
- `application.yml`
- Maven dependency management through the Spring Boot 2.7.18 parent

The application is deliberately small: a `Book` JPA entity and repository, a secured books API, a public endpoint, and a small RestTemplate client. This gives later exercises concrete call sites and dependencies to migrate.

## Run locally

Requires JDK 11 and Maven 3.6+.

```bash
mvn spring-boot:run
```

Public endpoint:

```bash
curl http://localhost:8080/api/public/hello
```

The books endpoints use HTTP Basic (`reader` / `rewrite-practice`):

```bash
curl -u reader:rewrite-practice http://localhost:8080/api/books
curl -u reader:rewrite-practice -H 'Content-Type: application/json' \
  -d '{"title":"OpenRewrite in Action","author":"Practice Author"}' \
  http://localhost:8080/api/books
```

The H2 console is available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:legacydb`, user `sa`, and a blank password.

## OpenRewrite migration

The root `rewrite.yml` defines `com.nikhil.migration.SpringBoot2To3Migration`.
It first runs the custom Java recipe
`com.nikhil.migration.MigrateLegacySecurityDsl`, then delegates the broader
framework and Jakarta changes to OpenRewrite's maintained
`org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0` recipe. The custom
recipe uses Spring Security type attribution to rename `authorizeRequests()`
and `antMatchers()` only when they resolve to the corresponding Spring Security
5 APIs. It deliberately leaves `RestTemplate` alone. The built-in recipe
upgrades Maven-managed Boot dependencies and moves the application to Java 17.

Build and install the custom recipe artifact before invoking the root project's
Maven plugin:

```bash
cd rewrite-recipes
mvn install
cd ..
```

The default recipe is the custom security DSL migration. Run a dry run to review
its proposed patch:

```bash
mvn rewrite:dryRun
```

Apply it after reviewing the diff:

```bash
mvn rewrite:run
```

To run the full composite, including the published Boot 3 migration recipe,
activate the catalog profile:

```bash
mvn -Pspring-migration-catalog rewrite:dryRun \
  -Drewrite.activeRecipes=com.nikhil.migration.SpringBoot2To3Migration
```

The published Spring migration recipe artifacts require Code Genome Project
access in some environments. Configure credentials in Maven's user-level `settings.xml` as
documented by [OpenRewrite's Spring Security recipe guide](https://docs.openrewrite.org/recipes/java/spring/security6/upgradespringsecurity_6_0),
then rerun Maven. Do not commit credentials.

The Boot migration is intentionally delegated to the published recipe catalog
instead of reimplementing framework-wide transformations. The custom DSL recipe
is a focused API rename; it does not restructure `WebSecurityConfigurerAdapter`
into a `SecurityFilterChain` or prove that authorization semantics are
equivalent. Review the resulting security configuration carefully and validate
the migrated application. `RestTemplate` is left in place because Boot 3 does
not require replacing it.

## Publish the custom recipe

The recipe module is prepared for Maven Central under the verified namespace
`io.github.nikv1811`. Before the first release, select a project license, add its
license file and metadata, and change the recipe version in
`rewrite-recipes/pom.xml` to a non-`SNAPSHOT` release version.

Configure a Central Portal user token in `~/.m2/settings.xml` under server ID
`central`, and make a GPG signing key available to GnuPG. Keep both credentials
and private keys outside this repository. Then deploy the recipe artifact:

```bash
cd rewrite-recipes
mvn -Pcentral-publish clean deploy
```

The `central-publish` profile attaches source and Javadoc JARs, signs the
deployment, and uploads it to the Central Portal. Review the validation result
and publish the deployment in the Portal. `autoPublish` is disabled so the
Portal does not publish a release before you review it.

After the release is available, set the consuming project's `recipe.version`
property to that released version. For example, with version `0.0.1`:

```xml
<dependency>
  <groupId>io.github.nikv1811</groupId>
  <artifactId>legacy-spring-boot2-openrewrite-recipes</artifactId>
  <version>0.0.1</version>
</dependency>
```

The root practice project's OpenRewrite plugin already loads this artifact as a
plugin dependency. Update its `recipe.version` property to use the Central
release instead of the local `0.0.1-SNAPSHOT` build.

## Suggested migration exercises

1. Upgrade the Maven parent and Java version to a Boot 3 / Java 17 target.
2. Migrate `javax.persistence` and `javax.validation` to `jakarta.*`.
3. Replace `WebSecurityConfigurerAdapter` with a `SecurityFilterChain` bean and migrate the legacy authorization DSL.
4. Keep `RestTemplate` initially, then separately evaluate an optional `RestClient`/`WebClient` migration.
5. Add one recipe and focused tests at a time; keep dependency upgrades, mechanical API edits, and behavior-sensitive security changes distinct.

This starter is for local learning. Its HTTP Basic credentials, disabled CSRF, and H2 console settings are intentionally simple and should not be used as production security configuration.

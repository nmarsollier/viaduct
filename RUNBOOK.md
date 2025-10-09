# Viaduct OSS Infrastructure Runbook

This document explains how to administer the infrastructure used by the
Viaduct OSS project.

## Github

Adminstration is handled by Airbnb's open source committee.

## CircleCI

We use CircleCI to run Viaduct's public [CI
jobs](https://app.circleci.com/pipelines/github/airbnb/viaduct).

### Invalidating CircleCI Cache

CircleCI sometimes fails due to issues with the Gradle cache. This can
be invalidated by changing the cache key is
[.circleci/config.yml](.circleci/config.yml).
[Example](https://github.com/airbnb/viaduct/commit/855dbda08f2dfb9a7fc58fe54ca8712a5d76fe8b)

## Gradle Plugin Portal

Plugins are published via the `viaduct-maintainers` account owned by
Airbnb. https://plugins.gradle.org/u/viaduct-maintainers

## Maven Central/Sonatype

Access to Airbnb's Sonatype namespace is controlled via Airbnb's Github
organization. Only members of the Airbnb Github organization can access
the namespace.

## Copybara

Viaduct has dual homes: Github and Airbnb's internal monorepo. We use
[Copybara](https://github.com/google/copybara) to sync changes between
the two source trees. Copybara runs on internal Airbnb infrastructure
and is not accessible to outside contributors.

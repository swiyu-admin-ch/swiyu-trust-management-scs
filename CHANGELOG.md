# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 3.19.2

### Changed

- Enable migration job to work with more than one DB

## 3.19.1

### Changed

- Utilize jEAP migration strategy resolver for DB migrations

## 3.19.0

### Added

- Adding published statement entity for trust protocol 2.0 statements

## 3.18.3

### Fixed

- Update jeap-spring-boot-parent to 31.4.0 to resolve CVE CVE-2026-22732

## 3.18.2

### Changed

- fixed NullPointerException when registryIds in trust statements have an empty value in "uid" field

## 3.18.1

### Changed

- throw exception if communication to CBS fails so the message gets into the Deadletter Queue instead of being marked as
  failed.

## 3.18.0

### Added

- Handle Trust Onboarding Add DID events

## 3.17.6

### Changed

- aligned audit-id for changes in audit metadata fields and auditing commands
- improved testability with kafka
- removed various mockings in integration tests

## 3.17.5

### Changed

- removed unused env variable

## 3.17.4

### Changed

- Move to aligned messaging error handling topic

## 3.17.3

### Fix

- Make linter work again and fix some linting issues

## 3.17.2

### Changed

- updating formatting depdendencies and jeap 30.19.0

## 3.17.1

### Changed

- Increase parent jeap-spring-boot-parent to 30.18.0 to fix CVE GHSA-72hv-8253-57qq

## 3.17.0

### Changed

- Add configurable functionality to automatically approve trust onboarding tasks for preview environment

## 3.16.0

### Changed

- updated java version from 21 to 25

## 3.15.6

### Changed

- updated jeap-spring-boot-parent to 30.15.0
- enabled support for detailed health metrics

## 3.15.5

### Changed

- removed duplicate correspondence language in view

## 3.15.4

### Added

- Added test for trust-onboarding-task-detail component

## 3.15.3

### Changed

- removed english in selection since we don't have english

## 3.15.2

### Changed

- removed self declaration in commercial register for partners other than individual
- i18n for partner type

## 3.15.1

### Added

- English is now selectable as UI language
- Removed unneeded css class sw-page-container and its usages

## 3.15.0

### Added

- Logout button & auth cleanup

## 3.14.3

### Changed

- Test: updated Pact tests GetTrustOnboardingSubmissionByIdPactConsumerTest to run with new semantic role ti_
  @trustonboardingsubmission_#read

## 3.14.2

### Changed

- renamed package of generated client apis so we can apply an arch rule on it which prevents accidentally exposing
  client apis

## 3.14.1

### Changed

- refactored generated model classes have now the Dto suffix

## 3.14.0

### Removed

- Drop PostgreSQL pg_trgm extension

## 3.13.0

### Added

- Added commercial register property to trust onboarding task detail

## 3.12.2

### Changed

- Improved TrustOnboardingSubmission Pact test
- fixed CVE-2025-66566 by updating "at.yawk.lz4:lz4-java" to "1.10.1"

## 3.12.1

### Changed

- Updated dependencies due to CVE-2025-66566

## 3.12.0

### Added

- Added EIAM IdP support

## 3.11.10

### Changed

- Fixed by CVE-2025-12183 by updating to lz4-java 1.8.1

## 3.11.9

### Added

- Added authorization-grant-type and scope to oauth2 client registration in application.yml

## 3.11.8

### Changed

- Update repo.bit.admin.ch:8444/postgres from 15.14 to 15.15
- Update au.com.dius.pact.provider:maven from 4.6.7 to 4.6.18
- Update au.com.dius.pact.consumer:junit5 from 4.6.17 to 4.6.18
- Update org.sonarsource.scanner.maven:sonar-maven-plugin from 5.2.0.4988 to 5.3.0.6276
- Update io.github.openfeign.querydsl:querydsl-jpa-spring from 7.0 to 7.1
- Update io.github.openfeign.querydsl:querydsl-apt from 7.0 to 7.1
- Update com.diffplug.spotless:spotless-maven-plugin from 3.0.0 to 3.1.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.16.0 to 7.17.0
- Update com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.20.0 to 2.20.1
- Update org.codehaus.mojo:exec-maven-plugin from 3.6.1 to 3.6.2
- Update org.openapitools:jackson-databind-nullable from 0.2.7 to 0.2.8
- Update com.authlete:sd-jwt from 1.5 to 1.7
- Update com.nimbusds:nimbus-jose-jwt from 10.5 to 10.6
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 27.4.0 to 28.3.0

## 3.11.7

### Changed

- Update core business service API client to latest version
- Introduce new business partner types
- Expand trust onboarding submission api concerning business partner type

## 3.11.6

### Changed

- Changed submission and due dates from localDate to instant

## 3.11.5

### Changed

- Upgraded to Angular 20 / Oblique 14, aligned code structure

## 3.11.4

### Added

- Add data-cy attributes

## 3.11.3

### Changed

- Remove milliseconds from trust statement issuing validFrom & validUntil as generic issuer cannot handle them

## 3.11.2

### Changed

- Frontend now reloads if the login status changes

## 3.11.1

### Added

- the credential request itself can now be logged by enabling trace level

## 3.11.0

### Added

- Documents are now listed and downloadable in TrustOnboardingTasks

## 3.10.5

### Changed

- kafka topics are now provided by event library and not configured in application.yml

## 3.10.4

### Changed

- fixed persistence issue on non-compliance-list

## 3.10.3

### Changed

- improved mapping of submission details from core business service

## 3.10.2

### Changed

- The Link to a partner (partnerId) is now persisted when issuing Trust statements

## 3.10.1

### Added

- fixed contact card in trust onboarding task

## 3.10.0

- added proof of possession date on did section in UI

## 3.9.0

### Added

- added Non-Compliance-List

## 3.8.1

### Added

- first example of consumer pact test

## 3.8.0

### Added

- added Non-Compliant-Actor API

## 3.7.4

### Changed

- reverted back to old gov issuer

## 3.7.3

### Changed

- coverage for ui is now reported to sonar

## 3.7.2

### Changed

- Update com.diffplug.spotless:spotless-maven-plugin from 2.46.1 to 3.0.0
- Update org.openapitools:openapi-generator-maven-plugin from 7.15.0 to 7.16.0
- Update com.fasterxml.uuid:java-uuid-generator from 5.1.0 to 5.1.1
- Update org.codehaus.mojo:exec-maven-plugin from 3.5.1 to 3.6.1
- Update org.codehaus.mojo:exec-maven-plugin from 3.5.1 to 3.6.1
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 27.3.0 to 27.4.0

## 3.7.1

### Changed

- Update maven from 3.9.10 to 3.9.11
- Update repo.bit.admin.ch:8444/postgres from 15.13 to 15.14
- Update repo.bit.admin.ch:8444/postgres from 15.13 to 15.14
- Update org.apache.maven.plugins:maven-surefire-plugin from 3.5.2 to 3.5.4
- Update com.rudikershaw.gitbuildhook:git-build-hook-maven-plugin from 3.5.0 to 3.6.0
- Update org.sonarsource.scanner.maven:sonar-maven-plugin from 5.1.0.4751 to 5.2.0.4988
- Update com.diffplug.spotless:spotless-maven-plugin from 2.45.0 to 2.46.1
- Update org.openapitools:openapi-generator-maven-plugin from 7.14.0 to 7.15.0
- Update com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.19.1 to 2.20.0
- Update org.openapitools:jackson-databind-nullable from 0.2.6 to 0.2.7
- Update com.nimbusds:nimbus-jose-jwt from 10.3.1 to 10.5
- Update org.bouncycastle:bcpkix-jdk18on from 1.81 to 1.82
- Update org.codehaus.mojo:exec-maven-plugin from 3.0.0 to 3.5.1
- Update org.apache.maven.plugins:maven-compiler-plugin from 3.14.0 to 3.14.1
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 26.75.1 to 27.2.0
- Update com.nimbusds:nimbus-jose-jwt from 10.3.1 to 10.5

## 3.7.0

### Changed

- migrated to new (merged) generic issuer

## 3.6.3

## Changed

- Update to latest jeap in order to fix CVE-2025-41248 and CVE-2025-41249

## 3.6.2

### Changed

- allow issuance of multiple truststatements of different types of canIssue/canVerify value for same subject

## 3.6.1

### Changed

- updated core business service API spec to latest version

## 3.6.0

### Added

- adding internal notes and assign to self on trust onboarding tasks

## 3.5.2

### Changed

- improved local development setup

## 3.5.1

### Changed

- Updated to latest jeap in order to fix CVE-2025-48989

## 3.5.0

### Added

- Tasks can now be approved and rejected. Also more information can be requested.
- Each Tasks shows now its history

## 3.4.0

### Added

- Adding TrustOnboardingTask Detail view

## 3.3.1

### Changed

- Publishing a VC schema that already exists no longer triggers an error, because this is considered a regular process
  scenario

## 3.3.0

### Added

- Added consumer/processor for TiTrustOnboardingSubmissionAcceptedEvent
- Introduced task repository and model (replacing mocked service implementations)

## 3.2.2

### Changed

- Update message dependencies to latest versions

## 3.2.1

### Changed

- Remove URL limitations for canIssue/Verify for TrustStatementIssuance/Verification issuance

## 3.2.0

### Added

- UI for task list (at the moment still with mocked data)

## 3.1.1

### Fixed

- Refactored migration script name to resolve duplicate

## 3.1.0

### Changed

- Refactored TrustStatementSubmission to TrustStatementPartnerLink and aligned naming and structure based on domain
  model updates.

## 3.0.4

### Changed

- Upgrade message dependency for failed and succeeded event because topic name was wrong

## 3.0.3

### Changed

- Configure kafka related settings to handle events and retrieve data via api client

## 3.0.2

- Using a business id for event messages as key

### Changed

- Allow vc schema paths to also include slashes

## 3.0.1

### Changed

- Allow vc schema paths to also include slashes

## 3.0.0

### Changed

- Moving whole repository structure to self-contained-system approach

## 2.3.0

### Added

- VcSchemaSubmissionAcceptedEvent is consumed and vc schema are stored

## 2.2.0

### Added

- Trust Statements from Trust Protocol V1 Spec are now
  supported (see https://github.com/admin-ch-ssi/specifications-to-publish/blob/feat/EID-4989/trust-protocol-v1.0.md)

## 2.1.4

### Other

- fixed CVE-2025-49146

## 2.1.3

### Other

- fixed a bug on database initialization

## 2.1.2

### Other

- resources that could not be found do not lead anymore to a 500 error, but to a 404

## 2.1.1

### Other

- various quality improvements and fixes

## 2.1.0

### Added

- Merged swiyu-registry-trust-authoring-service into this one (so API calls to former external service are replaced
  by internal calls)

## 2.0.9

### Other

- Update Interface Summaries

## 2.0.8

### Changed

- Downgrade maven-sunfire-plugin due to archunit incompatibility

## 2.0.7

### Changed

- Update org.springframework.security:spring-security-crypto from 6.4.5 to 6.5.0
- Remove unused org.hibernate.orm:hibernate-envers
- Update com.nimbusds:nimbus-jose-jwt from 9.48 to 10.3
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 26.50.0 to 26.50.1
- Update com.nimbusds:nimbus-jose-jwt from 9.48 to 10.3

## 2.0.6

### Changed

- Update maven from 3.9.3 to 3.9.9
- Update repo.bit.admin.ch:8444/postgres from 15.8 to 15.13
- Update org.springframework.security:spring-security-crypto from 6.4.4 to 6.4.5
- Update org.hibernate.orm:hibernate-envers from 6.6.1.Final to 6.6.15.Final
- Update org.openapitools:openapi-generator-maven-plugin from 7.10.0 to 7.13.0
- Update com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.18.3 to 2.19.0
- Update com.nimbusds:nimbus-jose-jwt from 9.47 to 9.48
- Update com.tngtech.archunit:archunit-junit5 from 1.3.0 to 1.4.1
- Update ch.admin.bit.jeap:jeap-spring-boot-parent from 26.47.0 to 26.50.0
- Update org.apache.maven.plugins:maven-compiler-plugin from 3.10.1 to 3.14.0

## 2.0.5

### Other

- Added spotless plugin

## 2.0.4

### Fixed

- Resolve CVE-2025-22235 by increasing jeap-spring-boot-parent to 26.47.0

## 2.0.3

### Fixed

- Subject field is added to make it possible by trust-data-service to filter credentials by subjects

## 2.0.2

### Fixed

- Fixes inconsistency in validFrom/validUntil handling
- Add statuslist correctly to issuing request

## 2.0.1

### Fixed

- Allow for status setting to be fully optional in update metadataV1 request

## 2.0.0

### Changed

- Refactored from CLI replacement into proper service

## 1.1.5

### Fixed

- Add support for bearer-jwt in openApi Config

## 1.1.4

### Changed

- Updated jeap spring boot parent version to 26.41.0
- Set version of spring-security-crypto specifically to 6.4.4 to resolve vulnerability CVE-2025-22228

## 1.1.3

### Fixed

- Minor quality improvements and fixes

## 1.1.2

### Fixed

- Requests to /actuator/prometheus are not any longer invoking health indicators

## 1.1.1

### Fixed

- Set connection timeout, read timeout and max redirects for rest client.

## 1.1.0

### Added

- Extending prometheus export with metrics for build

## 1.0.0

- Initial Release

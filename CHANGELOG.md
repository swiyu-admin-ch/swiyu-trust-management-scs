# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 3.32.0

### Added

- Add api definition and service for the ZAS SBN (Systematische Benutzer der AHV-Nr) for the sensitive data request

## 3.31.6

### Changed

- fix user role handling for int (preview) stages
- 
## 3.31.5

### Changed

- enabled default oblique spinner and error interceptor

## 3.31.4

### Changed

- Update error message when update statuslist fails

## 3.31.3

### Changed

- refactored jobs into dedicated module

## 3.31.2

### Changed

- Update maven from 3.9.12 to 3.9.16
- Update org.apache.maven.plugins:maven-surefire-plugin from 3.5.5 to 3.5.6
- Update ch.qos.logback:logback-core from 1.5.36 to 1.5.38
- Update org.sonarsource.scanner.maven:sonar-maven-plugin from 5.5.0.6356 to 5.7.0.6970
- Update io.github.openfeign.querydsl:querydsl-jpa-spring from 7.1 to 7.4.0
- Update io.github.openfeign.querydsl:querydsl-apt from 7.1 to 7.4.0
- Update com.diffplug.spotless:spotless-maven-plugin from 3.2.1 to 3.8.0
- Update com.fasterxml.jackson.datatype:jackson-datatype-jsr310 from 2.21.1 to 2.22.1
- Update org.openapitools:jackson-databind-nullable from 0.2.9 to 0.2.10
- Update com.authlete:sd-jwt from 1.7 to 1.9
- Update com.nimbusds:nimbus-jose-jwt from 10.8 to 10.9.1
- Update com.tngtech.archunit:archunit-junit5 from 1.4.1 to 1.4.2
- Update ch.admin.swiyu:swiyu-did-resolver-adapter from 1.4.0 to 1.7.0
- Update ch.admin.swiyu:swiyu-jws-signature-service from 1.4.0 to 1.7.0
- Update org.bouncycastle:bcpkix-jdk18on from 1.84 to 1.85
- Update org.apache.maven.plugins:maven-resources-plugin from 3.4.0 to 3.5.0
- Update com.nimbusds:nimbus-jose-jwt from 10.8 to 10.9.1

## 3.31.1

### Changed

- Update to Angular 21 and Oblique 15

## 3.31.0

### Added

- Support for local E2E tests

## 3.30.0

### Fixed

- Remove default language mapping from TrustStatementIdentityV1

## 3.29.5

### Changed

- Update README.md file

## 3.29.4

### Changed

- Add missing translation for trust onboarding tasks org name title en-CH

## 3.29.3

### Fixed

- Update pact test for swiyu-core-business-service to use en-CH instead for the organization name

## 3.29.2

### Fixed

- Force shell-quote,fast-uri,logback versions to fix HIGH vulnerability

## 3.29.1

### Changed

- Add missing language key en-CH which is used by swiyu-core-business-service
- Add missing open api spec for swiyu-trust-management-service ui

## 3.29.0

### Changed

- Adapt new trust onboarding localization of partner name from CBS.

## 3.28.5

### Changed

- Remove all HIGH and CRITICAL vulnerabilities

## 3.28.4

### Changed

- Switch from Base64 to Base64Url for JWT decoding

## 3.28.3

### Changed

- Fixing missing authorized_fields claim in pvaTS

## 3.28.2

### Changed

- version bump ch.admin.swiyu:didresolver to 2.8.2 (security fix, no CVE)

## 3.28.1

### Changed

- version bump jeap-spring-boot-parent to 33.11.0

## 3.28.0

### Removed

- Devops operations for trust anchor rollover is removed

## 3.27.6

### Changed

- Deactivation of old identity statements on trustOnboardingSubmission approval filtered for the subject

## 3.27.5

### Changed

- version bump spring-kafka to 3.3.16 (CVE-2026-41731)
- version bump jeap-spring-boot-parent to 33.9.0

## 3.27.4

### Changed

- Add deactivation of old identity statements on trustOnboardingSubmission approval

## 3.27.3

### Changed

- On HSM health test log DID resolve issues only as warnings

## 3.27.2

### Fixed

- Authentication context in message consumers now contains `iss` claim

## 3.27.1

### Fixed

- Fixed missing subject in VQPS payload.

## 3.27.0

### Added

- Audit command for nonCompliantActorAdded
- Audit command for nonCompliantActorDeleted
- Audit command for deactivateTrustStatement
- Audit command for publishTrustStatement
- Audit command for processVcSchemaSubmissionAccepted

## 3.26.2

### Fixed

- Fix VQPS publication failure when BCP-47 locale keys (e.g. `it-CH`, `de-CH`) are used: remove obsolete CBS
  Language-enum key conversion in `TrustStatementMapper` and pass localized maps directly from the CBS internal API to
  `VerificationQueryV2RequestDto`
- Align `ValidLocalizedMapValidator` with CBS: require a `"default"` key in all localized maps; remove early null-return
  so null maps are uniformly rejected
- Add explicit localized-map validation in `VqpsSubmissionEventProcessor` so invalid CBS payloads are caught before
  trust-statement issuance

## 3.26.1

### Changed

- Fixes SQL to remove MetadataV1 from DB to also remove linked foreign keys

## 3.26.0

### Removed

- Removing MetadataV1 capabilities

## 3.25.0

### Changed

- Remodel re-issue devops operations for trust anchor rollover

## 3.24.0

### Changed

- Switch both datasource connections (management-db, registry-db) to helm database-bindings pattern (CBS reference),
  removing manual vault env-var credential injection; add BIT-required afterMigrate.sql for object ownership
  reassignment on management-db

## 3.23.4

### Changed

- Improve Swagger examples for `@ValidLocalizedMap` fields to show the required `default` key alongside BCP 47 language
  codes (`de`, `fr`, `it`)

## 3.23.3

### Changed

- Consume TiVqpsSubmissionAcceptedEvent and publish VPQS to trust registry via event-driven flow; remove REST endpoint
  POST /api/v2/trust-statement-partner-links/verification-query

## 3.23.2

### Fixed

- fix default mapping for localized trust/public statement properties

## 3.23.1

### Changed

- Added tests and security for devops operations

## 3.23.0

### Added

- Added devops operation to re-issue all statements of a given type

## 3.22.2

### Changed

- Deactivate list statements in registry DB, not only management DB
- Optimise list statements re-issuing to prevent 404 on list statements in between

## 3.22.1

### Changed

- Deactivate list statements in registry DB, not only management DB
- Optimise list statements re-issuing to prevent 404 on list statements in between

## 3.22.0

### Changed

- Replaced all localized text fields across management API request DTOs with `Map<String, String>` using
  `@ValidLocalizedMap`, requiring a `"default"` key and open-ended BCP-47 locale keys. Removed `LocalizedTextDto`,
  `LocalizedTextValueSize`, `ValidLocaleKeys` and related infrastructure. Removed redundant `defaultLanguage` field from
  `IdentityV2RequestDto`.

## 3.21.3

### Changed

- removed storing a nonCompliantActorRemoved event when a protected issuance entry was deleted

## 3.21.2

### Changed

- Creation of ncTLS now available after adding/removing entry to non-compliance list
- Creation of piTLS now available after adding/removing entry to protected issuance list

## 3.21.1

### Changed

- fixed TrustStatementValidationFailedException when updating NonComplianceList statements (missing flush)
- fixed wrong API definition for ProtectedIssuanceEntry

## 3.21.0

### Added

- Add signatories in the TrustOnboardingTaskDetail

## 3.20.18

### Changed

- AsyncConfig propagates now the trace-id from parent to child thread
- improved testing of NonComplianceList publishing

## 3.20.17

### Changed

- Fixed noisy error logging of "missing entity name for default language". This issue arises when a trust onboarding
  takes place which does not have an entity name in the language of the partner's correspondence language.

## 3.20.16

### Changed

- Replace DidUtil::getDidFromKeyId with DidKt::getDidFromAbsoluteKid for more robust DID extraction from key identifiers

## 3.20.15

### Changed

- Add jti claim to idTS and ncTLS
- Enforce max sizes on piaTS

## 3.20.14

### Changed

- AddDid Flow can now handle that multiple statements might present the identity

## 3.20.12

### Changed

- use userPin instead of keyPin for securosys HSM support

## 3.20.11

### Changed

- Aligning and clarifying various class and variable namings
- Remove status from Statement and use descriptive flags
- Set deactivation from management db in registry db
- Clarify namings and structure from JwtIssuerConfiguration/Properties
- Move magic numbers statuslist size, statuslist expiry and statement expiry into configuration
- Aligning controller interface numbering

## 3.20.10

### Added

- Add capability to issue Token Status Lists for TP2.0 Statements

## 3.20.9

### Added

- Add HSM JWT signing capability for TP2.0 Statements

## 3.20.8

### Added

- Add capability to issue TP2.0 Verification Query Public Statements

## 3.20.7

### Added

- Add capability to issue TP2.0 Protected Issuance Trust List Statement

## 3.20.6

### Added

- Add capability to add localized reason and vct_name to TP2.0 Protected Issuance Authorization Trust Statement

## 3.20.5

### Added

- Add capability to issue TP2.0 Non-Compliance Trust List Statement
- TP2.0 Non-Compliance Trust List Statement is now issued after Non-Compliance Actor creation and deletion

## 3.20.4

### Added

- Add capability to issue TP2.0 Protected Issuance Authorization Trust Statement

## 3.20.3

### Added

- Add capability to issue TP2.0 Protected Verification Authorization Trust Statement

## 3.20.2

### Added

- TP2.0 Identity Trust Statements are now issued after TrustOnboardingSubmission and AddDidSubmission approvals

## 3.20.1

### Added

- Add capability to issue TP2.0 Identity Trust Statements

## 3.20.0

### Added

- Add capability to create JWTs

## 3.19.9

### Changed

- Remove isGovActor fallback handling in trust onboarding task mapper

## 3.19.8

### Changed

- Updated setVersions script to ensure -SNAPSHOT suffix and CHANGELOG entry
- Added .git-hooks/pre-commit to block commits without entries in CHANGELOG.md

## 3.19.7

### Changed

- fixed various sonar findings
- testing: upgraded to PostgreSQLContainerInitializer to use postgres 17 instead of 15

## 3.19.6

### Changed

- version bump of basic-ftp to 5.2.0 to fix CVE-2026-27699

## 3.19.5

### Changed

- Updated to JEAP 33.6.0 which fixes broken health indicator metrics for multi db

## 3.19.4

### Added

- Added css / scss linting and formatting

## 3.19.3

### Added

- Add auditing fields to new published_statements table

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

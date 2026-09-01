export * from './business-partner-identity-api';
export * from './domain-event-log-api';
export * from './frontend-configuration-api';
export * from './protected-issuance-authorization-api';
export * from './protected-verification-authorization-api';
export * from './task-api';
export * from './trust-onboarding-document-api';
export * from './trust-statement-issuance-api';
import {BusinessPartnerIdentityApi} from './business-partner-identity-api';
import {DomainEventLogApi} from './domain-event-log-api';
import {FrontendConfigurationApi} from './frontend-configuration-api';
import {ProtectedIssuanceAuthorizationApi} from './protected-issuance-authorization-api';
import {ProtectedVerificationAuthorizationApi} from './protected-verification-authorization-api';
import {TaskApi} from './task-api';
import {TrustOnboardingDocumentApi} from './trust-onboarding-document-api';
import {TrustStatementIssuanceApi} from './trust-statement-issuance-api';
export const APIS = [
  BusinessPartnerIdentityApi,
  DomainEventLogApi,
  FrontendConfigurationApi,
  ProtectedIssuanceAuthorizationApi,
  ProtectedVerificationAuthorizationApi,
  TaskApi,
  TrustOnboardingDocumentApi,
  TrustStatementIssuanceApi
];

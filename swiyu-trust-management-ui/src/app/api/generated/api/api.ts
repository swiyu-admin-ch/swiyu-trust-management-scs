export * from './domain-event-log-api';
export * from './frontend-configuration-api';
export * from './trust-onboarding-document-api';
export * from './trust-onboarding-task-api';
import {DomainEventLogApi} from './domain-event-log-api';
import {FrontendConfigurationApi} from './frontend-configuration-api';
import {TrustOnboardingDocumentApi} from './trust-onboarding-document-api';
import {TrustOnboardingTaskApi} from './trust-onboarding-task-api';
export const APIS = [DomainEventLogApi, FrontendConfigurationApi, TrustOnboardingDocumentApi, TrustOnboardingTaskApi];

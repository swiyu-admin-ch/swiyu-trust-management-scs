import {Clipboard} from '@angular/cdk/clipboard';
import {HttpResponse, provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {
  ObDocumentMetaService,
  ObENotificationType,
  ObNotificationService,
  provideObliqueTestingConfiguration
} from '@oblique/oblique';
import {of} from 'rxjs';
import {
  Language,
  PagedModelTrustOnboardingSubmissionDocumentListItemDto,
  PartnerType,
  TrustOnboardingDocumentApi,
  TrustOnboardingTask,
  TrustOnboardingTaskAction,
  TrustOnboardingTaskApi,
  TrustOnboardingTaskStatus
} from '../../api/generated';
import {TrustOnboardingTaskDetailComponent} from './trust-onboarding-task-detail.component';

describe('TrustOnboardingTaskComponent', () => {
  let fixture: ComponentFixture<TrustOnboardingTaskDetailComponent>;
  let component: TrustOnboardingTaskDetailComponent;
  let mockApi: jest.Mocked<TrustOnboardingTaskApi>;
  let mockDocumentsApi: jest.Mocked<TrustOnboardingDocumentApi>;
  let mockObNotificationService: jest.Mocked<ObNotificationService>;
  let mockObDocumentMetaService: jest.Mocked<ObDocumentMetaService>;
  let mockClipboard: jest.Mocked<Clipboard>;

  beforeEach(async () => {
    mockObDocumentMetaService = {
      setTitle: jest.fn()
    } as unknown as jest.Mocked<ObDocumentMetaService>;

    mockApi = {
      getTask: jest.fn(),
      assignSelf: jest.fn()
    } as unknown as jest.Mocked<TrustOnboardingTaskApi>;

    mockDocumentsApi = {
      getTrustOnboardingSubmissionDocuments: jest.fn()
    } as unknown as jest.Mocked<TrustOnboardingDocumentApi>;

    mockObNotificationService = {
      send: jest.fn()
    } as unknown as jest.Mocked<ObNotificationService>;

    mockClipboard = {
      copy: jest.fn()
    } as unknown as jest.Mocked<Clipboard>;

    await TestBed.configureTestingModule({
      imports: [TrustOnboardingTaskDetailComponent],

      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideObliqueTestingConfiguration(),

        {provide: TrustOnboardingTaskApi, useValue: mockApi},
        {provide: TrustOnboardingDocumentApi, useValue: mockDocumentsApi},
        {provide: ObNotificationService, useValue: mockObNotificationService},
        {provide: ObDocumentMetaService, useValue: mockObDocumentMetaService},
        {provide: Clipboard, useValue: mockClipboard}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrustOnboardingTaskDetailComponent);
    component = fixture.componentInstance;
  });

  it('should load task when taskId is set', () => {
    const mockTask = getTestTask('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    mockApi.getTask.mockReturnValue(
      of(
        new HttpResponse<TrustOnboardingTask>({
          body: mockTask
        })
      )
    );
    mockDocumentsApi.getTrustOnboardingSubmissionDocuments.mockReturnValue(
      of(
        new HttpResponse<PagedModelTrustOnboardingSubmissionDocumentListItemDto>({
          body: testDocumentsData
        })
      )
    );

    fixture.componentRef.setInput('taskId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    expect(mockApi.getTask).toHaveBeenCalledWith({taskId: 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd'});
  });

  it('should share link and send notification', () => {
    // Translations do currently not work during unit tests
    const mockTitle = 'app.trust-onboarding-task.actions.share-link.notification.title';
    // after translations work check that the mockUrl is part of the mockMessage
    const mockMessage = 'app.trust-onboarding-task.actions.share-link.notification.message';
    const mockUrl = 'http://localhost/';

    window.history.replaceState(null, '', mockUrl);

    component.shareLink();

    expect(mockClipboard.copy).toHaveBeenCalledWith(mockUrl);
    expect(mockObNotificationService.send).toHaveBeenCalledWith(
      {title: mockTitle, message: mockMessage},
      ObENotificationType.INFO
    );
  });
  it('should call assignSelf API and reload task', () => {
    const mockTask = getTestTask('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    mockApi.getTask.mockReturnValue(of(mockTask as unknown as HttpResponse<TrustOnboardingTask>));
    mockDocumentsApi.getTrustOnboardingSubmissionDocuments.mockReturnValue(
      of(new HttpResponse<PagedModelTrustOnboardingSubmissionDocumentListItemDto>({body: testDocumentsData}))
    );

    fixture.componentRef.setInput('taskId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    // component is now ready and has loaded the task
    (component.domainEventList.reloadEvents as jest.Mock) = jest.fn();
    mockApi.assignSelf.mockReturnValue(of(new HttpResponse({body: {}})));

    component.assignSelf(mockTask);

    expect(mockApi.assignSelf).toHaveBeenCalledWith({taskId: mockTask.id});
    expect(component.domainEventList.reloadEvents).toHaveBeenCalled();
  });
  it('should show the inCommercialRegister block when partnerType is BUSINESS', () => {
    const mockTask = getTestTaskWithPartnerType('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd', PartnerType.Business);

    mockApi.getTask.mockReturnValue(of(mockTask as unknown as HttpResponse<TrustOnboardingTask>));
    mockDocumentsApi.getTrustOnboardingSubmissionDocuments.mockReturnValue(
      of(new HttpResponse<PagedModelTrustOnboardingSubmissionDocumentListItemDto>({body: testDocumentsData}))
    );

    fixture.componentRef.setInput('taskId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    const el = fixture.nativeElement.querySelector('[data-cy="inCommercialRegisterBlock"]');

    expect(el).toBeTruthy();
  });
  it('should NOT show inCommercialRegister block when partnerType is GOV', () => {
    const mockTask = getTestTaskWithPartnerType(
      'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd',
      PartnerType.GovernmentalInstitution
    );

    mockApi.getTask.mockReturnValue(of(mockTask as unknown as HttpResponse<TrustOnboardingTask>));
    mockDocumentsApi.getTrustOnboardingSubmissionDocuments.mockReturnValue(
      of(new HttpResponse<PagedModelTrustOnboardingSubmissionDocumentListItemDto>({body: testDocumentsData}))
    );

    fixture.componentRef.setInput('taskId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    const el = fixture.nativeElement.querySelector('[data-cy="inCommercialRegisterBlock"]');

    expect(el).toBeNull();
  });
  it('should NOT show inCommercialRegister block when partnerType is INDIVIDUAL', () => {
    const mockTask = getTestTaskWithPartnerType('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd', PartnerType.Individual);

    mockApi.getTask.mockReturnValue(of(mockTask as unknown as HttpResponse<TrustOnboardingTask>));
    mockDocumentsApi.getTrustOnboardingSubmissionDocuments.mockReturnValue(
      of(new HttpResponse<PagedModelTrustOnboardingSubmissionDocumentListItemDto>({body: testDocumentsData}))
    );

    fixture.componentRef.setInput('taskId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    const el = fixture.nativeElement.querySelector('[data-cy="inCommercialRegisterBlock"]');

    expect(el).toBeNull();
  });
});

const testDocumentsData: PagedModelTrustOnboardingSubmissionDocumentListItemDto = {
  content: [
    {
      id: 'ce9a7f76-86f7-4503-8f7c-e29b8f12f6b9',
      name: 'Handelsregisterauszug.pdf',
      mediaType: 'application/pdf',
      type: 'TRUST_ONBOARDING_OTHER',
      owningBusinessPartner: 'bbdcd197-7794-4333-a9d1-7b97896d0ef1',
      createdAt: '2025-11-01T10:15:00Z',
      updatedAt: '2025-11-02T14:30:00Z',
      submittedAt: '2025-11-03T09:00:00Z',
      trustOnboardingSubmissionId: '39b190e6-9132-4506-888d-11f361679649'
    },
    {
      id: '04eca05e-fca4-44c9-b847-38aa437e2843',
      name: 'Declaration of intent.pdf',
      mediaType: 'application/pdf',
      type: 'TRUST_ONBOARDING_DECLARATION_OF_INTENT',
      owningBusinessPartner: 'bbdcd197-7794-4333-a9d1-7b97896d0ef1',
      createdAt: '2025-10-28T08:45:00Z',
      updatedAt: '2025-10-29T12:00:00Z',
      submittedAt: '2025-10-30T16:20:00Z',
      trustOnboardingSubmissionId: '39b190e6-9132-4506-888d-11f361679649'
    },
    {
      id: '7fb39153-8b2e-40d2-b4a3-6360b42b9c0f',
      name: 'Unterschriebener Vertrag.pdf',
      mediaType: 'application/pdf',
      type: 'TRUST_ONBOARDING_OTHER',
      owningBusinessPartner: 'bbdcd197-7794-4333-a9d1-7b97896d0ef1',
      createdAt: '2025-11-05T07:30:00Z',
      updatedAt: '2025-11-06T11:10:00Z',
      submittedAt: '2025-11-06T15:45:00Z',
      trustOnboardingSubmissionId: '39b190e6-9132-4506-888d-11f361679649'
    }
  ],
  page: {
    size: 3,
    number: 0,
    totalElements: 3,
    totalPages: 1
  }
};

function getTestTask(id: string): TrustOnboardingTask {
  return {
    id: id,
    assignee: undefined,
    submittedAt: '2025-08-18T00:00:00Z',
    dueAt: '2028-05-15T00:00:00Z',
    state: TrustOnboardingTaskStatus.Accepted,
    partnerType: PartnerType.GovernmentalInstitution,
    uid: undefined,
    correspondenceLanguage: Language.DeCh,
    entityName: {
      default: 'test de',
      'de-CH': 'test de',
      'it-CH': 'test it',
      'rm-CH': 'test rm',
      en: 'test en',
      'fr-CH': 'test fr'
    },
    address: undefined,
    zipCodeCity: undefined,
    country: undefined,
    email: 'test@example.email.admin.ch',
    contacts: [],
    dids: [],
    allowedActions: new Set([
      TrustOnboardingTaskAction.Approve,
      TrustOnboardingTaskAction.Reject,
      TrustOnboardingTaskAction.RequestMoreInformation
    ])
  };
}

function getTestTaskWithPartnerType(id: string, type: PartnerType, isRegisteredInCommercialRegister = true) {
  const task = getTestTask(id);
  task.partnerType = type;
  task.isRegisteredInCommercialRegister = isRegisteredInCommercialRegister;
  return task;
}

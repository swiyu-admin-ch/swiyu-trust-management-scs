import {Clipboard} from '@angular/cdk/clipboard';
import {HttpResponse, provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {
  ObDocumentMetaService,
  ObENotificationType,
  ObNotificationService,
  provideObliqueTestingConfiguration
} from '@oblique/oblique';
import {of, throwError} from 'rxjs';
import {NonCompliantActor, NonCompliantActorApi} from '../../api/generated';
import {NonComplianceDetailComponent} from './non-compliance-detail.component';

describe('NonComplianceDetailComponent', () => {
  let fixture: ComponentFixture<NonComplianceDetailComponent>;
  let component: NonComplianceDetailComponent;
  let mockApi: jest.Mocked<NonCompliantActorApi>;
  let mockObNotificationService: jest.Mocked<ObNotificationService>;
  let mockObDocumentMetaService: jest.Mocked<ObDocumentMetaService>;
  let mockClipboard: jest.Mocked<Clipboard>;

  beforeEach(async () => {
    mockObDocumentMetaService = {
      setTitle: jest.fn()
    } as unknown as jest.Mocked<ObDocumentMetaService>;

    mockApi = {
      getNonCompliantActor: jest.fn()
    } as unknown as jest.Mocked<NonCompliantActorApi>;

    mockObNotificationService = {
      send: jest.fn()
    } as unknown as jest.Mocked<ObNotificationService>;

    mockClipboard = {
      copy: jest.fn()
    } as unknown as jest.Mocked<Clipboard>;

    await TestBed.configureTestingModule({
      imports: [NonComplianceDetailComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideObliqueTestingConfiguration(),
        {provide: NonCompliantActorApi, useValue: mockApi},
        {provide: ObNotificationService, useValue: mockObNotificationService},
        {provide: ObDocumentMetaService, useValue: mockObDocumentMetaService},
        {provide: Clipboard, useValue: mockClipboard}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NonComplianceDetailComponent);
    component = fixture.componentInstance;
  });

  it('should load actor when actorId is set', () => {
    const mockActor = getTestActor('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    mockApi.getNonCompliantActor.mockReturnValue(of(mockActor));

    fixture.componentRef.setInput('actorId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    expect(mockApi.getNonCompliantActor).toHaveBeenCalledWith({
      nonCompliantActorId: 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd'
    });
    expect(component.actor().did).toBe('did:test:abc');
  });

  it('should set notFoundError and reset the title on 404', () => {
    mockApi.getNonCompliantActor.mockReturnValue(
      throwError(() => new HttpResponse<NonCompliantActor>({body: null, status: 404}))
    );

    fixture.componentRef.setInput('actorId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    expect(component.notFoundError()).toBe(true);
    expect(mockObDocumentMetaService.setTitle).toHaveBeenCalledWith('app.menu.nonComplianceActors');
  });

  it('should set the title to the actor identifier on success', () => {
    const mockActor = getTestActor('a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    mockApi.getNonCompliantActor.mockReturnValue(of(mockActor));

    fixture.componentRef.setInput('actorId', 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd');
    fixture.detectChanges();

    expect(mockObDocumentMetaService.setTitle).toHaveBeenCalledWith('did:test:abc');
  });

  it('should share link and send a single notification', () => {
    const mockTitle = 'app.trust-onboarding-task.actions.share-link.notification.title';
    const mockMessage = 'app.trust-onboarding-task.actions.share-link.notification.message';
    const mockUrl = 'http://localhost/';

    window.history.replaceState(null, '', mockUrl);

    component.shareLink();

    expect(mockClipboard.copy).toHaveBeenCalledWith(mockUrl);
    expect(mockObNotificationService.send).toHaveBeenCalledTimes(1);
    expect(mockObNotificationService.send).toHaveBeenCalledWith(
      {title: mockTitle, message: mockMessage},
      ObENotificationType.INFO
    );
  });
});

function getTestActor(id: string): NonCompliantActor {
  return {
    id: id,
    did: 'did:test:abc',
    businessPartnerId: '1000000001',
    flaggedAsNonCompliantAt: '2026-01-01T00:00:00Z',
    reason: {
      reasonDe: 'Grund',
      reasonEn: 'Violation',
      reasonFr: 'Motif',
      reasonIt: 'Motivo',
      reasonRm: 'Motiv'
    }
  };
}

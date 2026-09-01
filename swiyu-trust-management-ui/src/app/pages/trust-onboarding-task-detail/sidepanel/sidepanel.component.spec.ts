import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideTranslateService} from '@ngx-translate/core';
import {of} from 'rxjs';
import {TaskAction, TaskApi, TaskListItem, TaskStatus, TrustOnboardingRejectReason} from '../../../api/generated';
import {SidepanelComponent} from './sidepanel.component';
import {SidepanelService} from './sidepanel.service';

describe('SidepanelComponent', () => {
  let component: SidepanelComponent;
  let fixture: ComponentFixture<SidepanelComponent>;
  let sidepanelService: SidepanelService;
  let mockApi: jest.Mocked<TaskApi>;

  const taskId = 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd';

  function getTaskListItem(taskType: string): TaskListItem {
    return {
      id: taskId,
      partnerName: {default: 'Acme AG'},
      submittedAt: '2026-01-01T00:00:00Z',
      dueAt: '2026-02-01T00:00:00Z',
      state: TaskStatus.Opened,
      taskType,
      allowedActions: new Set([TaskAction.Approve, TaskAction.Reject])
    };
  }

  beforeEach(async () => {
    mockApi = {
      approveTrustOnboarding: jest.fn().mockReturnValue(of(undefined)),
      rejectTrustOnboarding: jest.fn().mockReturnValue(of(undefined)),
      approveProtectedVerificationRequest: jest.fn().mockReturnValue(of(undefined)),
      rejectProtectedVerificationRequest: jest.fn().mockReturnValue(of(undefined))
    } as unknown as jest.Mocked<TaskApi>;

    await TestBed.configureTestingModule({
      imports: [SidepanelComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideTranslateService(),
        {provide: TaskApi, useValue: mockApi}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SidepanelComponent);
    component = fixture.componentInstance;
    sidepanelService = TestBed.inject(SidepanelService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('calls the onboarding approve endpoint for a non-protected-verification task', () => {
    sidepanelService.approve(getTaskListItem('ONBOARDING'));
    fixture.detectChanges();
    component.formData().declarationAccepted = true;

    component.submit({invalid: false} as never);

    expect(mockApi.approveTrustOnboarding).toHaveBeenCalledWith({
      taskId,
      request: {internalNote: '', partnerNote: ''}
    });
    expect(mockApi.approveProtectedVerificationRequest).not.toHaveBeenCalled();
  });

  it('calls the protected-verification approve endpoint for a protected-verification task', () => {
    sidepanelService.approve(getTaskListItem('PROTECTED_VERIFICATION_REQUEST'));
    fixture.detectChanges();

    component.submit({invalid: false} as never);

    expect(mockApi.approveProtectedVerificationRequest).toHaveBeenCalledWith({
      taskId,
      request: {internalNote: ''}
    });
    expect(mockApi.approveTrustOnboarding).not.toHaveBeenCalled();
  });

  it('calls the onboarding reject endpoint with the selected dropdown reason for a non-protected-verification task', () => {
    sidepanelService.reject(getTaskListItem('ONBOARDING'));
    fixture.detectChanges();
    component.formData().rejectReason = TrustOnboardingRejectReason.Other;
    component.formData().declarationAccepted = true;

    component.submit({invalid: false} as never);

    expect(mockApi.rejectTrustOnboarding).toHaveBeenCalledWith({
      taskId,
      request: {internalNote: '', partnerNote: '', rejectReason: TrustOnboardingRejectReason.Other}
    });
    expect(mockApi.rejectProtectedVerificationRequest).not.toHaveBeenCalled();
  });

  it('calls the protected-verification reject endpoint with the free-text reason for a protected-verification task', () => {
    sidepanelService.reject(getTaskListItem('PROTECTED_VERIFICATION_REQUEST'));
    fixture.detectChanges();
    component.formData().protectedVerificationRejectReason = 'Not eligible';

    component.submit({invalid: false} as never);

    expect(mockApi.rejectProtectedVerificationRequest).toHaveBeenCalledWith({
      taskId,
      request: {internalNote: '', rejectReason: 'Not eligible'}
    });
    expect(mockApi.rejectTrustOnboarding).not.toHaveBeenCalled();
  });

  it('does not reject a protected-verification task without a free-text reason', () => {
    sidepanelService.reject(getTaskListItem('PROTECTED_VERIFICATION_REQUEST'));
    fixture.detectChanges();
    component.formData().protectedVerificationRejectReason = '   ';

    component.submit({invalid: false} as never);

    expect(mockApi.rejectProtectedVerificationRequest).not.toHaveBeenCalled();
  });

  it('hides the declaration checkbox for protected-verification tasks', () => {
    sidepanelService.approve(getTaskListItem('PROTECTED_VERIFICATION_REQUEST'));
    fixture.detectChanges();

    expect(component.declarationCheckVisible()).toBe(false);
  });
});

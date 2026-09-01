import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ObDocumentMetaService, provideObliqueTestingConfiguration} from '@oblique/oblique';
import {of} from 'rxjs';
import {
  DomainEventLogApi,
  ProtectedVerificationRequestTask,
  TaskAction,
  TaskApi,
  TaskStatus
} from '../../api/generated';
import {ProtectedVerificationRequestTaskDetailComponent} from './protected-verification-request-task-detail.component';

describe('ProtectedVerificationRequestTaskDetailComponent', () => {
  let fixture: ComponentFixture<ProtectedVerificationRequestTaskDetailComponent>;
  let component: ProtectedVerificationRequestTaskDetailComponent;
  let mockApi: jest.Mocked<TaskApi>;
  let mockDomainEventLogApi: jest.Mocked<DomainEventLogApi>;
  let mockObDocumentMetaService: jest.Mocked<ObDocumentMetaService>;

  const taskId = 'a4a92559-21cc-4ed0-8053-d3c78bb5b5cd';

  function getTestTask(overrides: Partial<ProtectedVerificationRequestTask> = {}): ProtectedVerificationRequestTask {
    return {
      id: taskId,
      submittedAt: '2026-01-01T00:00:00Z',
      dueAt: '2026-02-01T00:00:00Z',
      state: TaskStatus.Opened,
      partnerName: {default: 'Acme AG'},
      category: ProtectedVerificationRequestTask.CategoryEnum.AhvNumber,
      zasDataOpened: false,
      allowedActions: new Set([TaskAction.Approve, TaskAction.Reject]),
      protectedVerificationSubmissionId: 'b4a92559-21cc-4ed0-8053-d3c78bb5b5cd',
      ...overrides
    };
  }

  beforeEach(async () => {
    mockObDocumentMetaService = {
      setTitle: jest.fn()
    } as unknown as jest.Mocked<ObDocumentMetaService>;

    mockApi = {
      getProtectedVerificationRequestTask: jest.fn(),
      getProtectedVerificationRequestTaskZasData: jest.fn(),
      approveProtectedVerificationRequest: jest.fn(),
      rejectProtectedVerificationRequest: jest.fn()
    } as unknown as jest.Mocked<TaskApi>;

    mockDomainEventLogApi = {
      getDomainEventLogs: jest.fn().mockReturnValue(of({content: [], page: {totalElements: 0}}))
    } as unknown as jest.Mocked<DomainEventLogApi>;

    await TestBed.configureTestingModule({
      imports: [ProtectedVerificationRequestTaskDetailComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideObliqueTestingConfiguration(),
        {provide: TaskApi, useValue: mockApi},
        {provide: DomainEventLogApi, useValue: mockDomainEventLogApi},
        {provide: ObDocumentMetaService, useValue: mockObDocumentMetaService}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProtectedVerificationRequestTaskDetailComponent);
    component = fixture.componentInstance;
  });

  it('should load task when taskId is set', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask()) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    expect(mockApi.getProtectedVerificationRequestTask).toHaveBeenCalledWith({taskId});
    expect(component.task().id).toBe(taskId);
  });

  it('should block approve/reject until ZAS data has been opened in this session', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask({zasDataOpened: false})) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    expect(component.canDecide()).toBe(false);

    component.onZasDataOpened();

    expect(component.canDecide()).toBe(true);
  });

  it('should refresh allowedActions after opening ZAS data, so Approve/Reject become enabled without a page reload', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValueOnce(
      of(getTestTask({zasDataOpened: false, allowedActions: new Set()})) as never
    );

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    expect(component.isActionAllowed(TaskAction.Approve)).toBe(false);

    mockApi.getProtectedVerificationRequestTask.mockReturnValueOnce(
      of(
        getTestTask({
          zasDataOpened: true,
          allowedActions: new Set([TaskAction.Approve, TaskAction.Reject])
        })
      ) as never
    );

    component.onZasDataOpened();

    expect(mockApi.getProtectedVerificationRequestTask).toHaveBeenCalledTimes(2);
    expect(component.isActionAllowed(TaskAction.Approve)).toBe(true);
  });

  it('should allow deciding immediately if the task already has zasDataOpened set by the backend', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask({zasDataOpened: true})) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    expect(component.canDecide()).toBe(true);
  });

  it('should approve with the entered internal note', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask()) as never);
    mockApi.approveProtectedVerificationRequest.mockReturnValue(of(undefined) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    component.internalNote = 'looks good';
    component.approve();

    expect(mockApi.approveProtectedVerificationRequest).toHaveBeenCalledWith({
      taskId,
      request: {internalNote: 'looks good'}
    });
  });

  it('should not reject without a reject reason', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask()) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    component.rejectReason = '   ';
    component.reject();

    expect(mockApi.rejectProtectedVerificationRequest).not.toHaveBeenCalled();
  });

  it('should reject with the entered reason and internal note', () => {
    mockApi.getProtectedVerificationRequestTask.mockReturnValue(of(getTestTask()) as never);
    mockApi.rejectProtectedVerificationRequest.mockReturnValue(of(undefined) as never);

    fixture.componentRef.setInput('taskId', taskId);
    fixture.detectChanges();

    component.rejectReason = 'Not eligible';
    component.internalNote = 'checked twice';
    component.reject();

    expect(mockApi.rejectProtectedVerificationRequest).toHaveBeenCalledWith({
      taskId,
      request: {rejectReason: 'Not eligible', internalNote: 'checked twice'}
    });
  });
});

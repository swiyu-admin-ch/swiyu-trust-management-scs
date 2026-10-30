import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {of, throwError} from 'rxjs';
import {DeleteBusinessPartnerTask, DomainEventLogApi, TaskAction, TaskApi, TaskStatus} from '../../api/generated';
import {SidepanelService} from '../trust-onboarding-task-detail/sidepanel/sidepanel.service';
import {DeleteBusinessPartnerTaskDetailComponent} from './delete-business-partner-task-detail.component';

describe('DeleteBusinessPartnerTaskDetailComponent', () => {
  let fixture: ComponentFixture<DeleteBusinessPartnerTaskDetailComponent>;
  let component: DeleteBusinessPartnerTaskDetailComponent;
  let api: jest.Mocked<Pick<TaskApi, 'getDeleteBusinessPartnerTask'>>;
  let panel: SidepanelService;
  const task: DeleteBusinessPartnerTask = {
    id: 'task-1',
    businessPartnerId: 'partner-1',
    partnerName: 'Acme AG',
    submittedAt: '2026-10-21T10:00:00Z',
    hardDeleteDueAt: '2026-10-23T10:00:00Z',
    state: TaskStatus.Opened,
    taskType: 'DELETE_BUSINESS_PARTNER',
    allowedActions: new Set([TaskAction.Reject])
  };

  beforeEach(async () => {
    api = {getDeleteBusinessPartnerTask: jest.fn().mockReturnValue(of(task))};
    await TestBed.configureTestingModule({
      imports: [DeleteBusinessPartnerTaskDetailComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        {provide: TaskApi, useValue: api},
        {provide: DomainEventLogApi, useValue: {getDomainEventLogs: jest.fn().mockReturnValue(of({content: []}))}}
      ]
    }).compileComponents();
    TestBed.inject(TranslateService).use('en');
    fixture = TestBed.createComponent(DeleteBusinessPartnerTaskDetailComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('taskId', 'task-1');
    panel = TestBed.inject(SidepanelService);
    fixture.detectChanges();
    fixture.detectChanges();
  });

  it('loads the deletion detail and renders the deadline, even without a generic dueAt', () => {
    expect(api.getDeleteBusinessPartnerTask).toHaveBeenCalledWith({taskId: 'task-1'});
    expect(fixture.nativeElement.textContent).toContain('Acme AG');
    expect(fixture.nativeElement.querySelector('[data-cy="hardDeleteDueAt"]').textContent).toContain('23.10.2026');
    expect(fixture.nativeElement.querySelector('[data-cy="btnTaskApprove"]')).toBeNull();
  });

  it('opens rejection for this task and refreshes its state after the panel closes', () => {
    const reject = jest.spyOn(panel, 'reject');
    component.reject();
    expect(reject).toHaveBeenCalledWith(task);
    api.getDeleteBusinessPartnerTask.mockReturnValue(
      of({...task, state: TaskStatus.Rejected, allowedActions: new Set()})
    );
    panel.closePanel();
    fixture.detectChanges();
    expect(component.canReject).toBe(false);
    expect(fixture.nativeElement.querySelector('[data-cy="btnTaskReject"]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-cy="hardDeleteDueAt"]').textContent).toContain('23.10.2026');
    component.reject();
    expect(reject).toHaveBeenCalledTimes(1);
  });

  it('handles a missing task and can load a different task afterwards', () => {
    api.getDeleteBusinessPartnerTask.mockReturnValue(throwError(() => ({status: 404})));
    fixture.componentRef.setInput('taskId', 'missing');
    fixture.detectChanges();
    expect(component.notFound()).toBe(true);
    expect(component.task()).toBeNull();
    api.getDeleteBusinessPartnerTask.mockReturnValue(of(task));
    fixture.componentRef.setInput('taskId', 'task-1');
    fixture.detectChanges();
    expect(component.notFound()).toBe(false);
    expect(component.task()?.id).toBe('task-1');
  });
});

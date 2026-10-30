import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {of} from 'rxjs';
import {TaskAction, TaskApi, TaskListItem, TaskStatus} from '../../api/generated';
import {routes} from '../../app.routes';
import {DeleteBusinessPartnerTaskDetailComponent} from '../delete-business-partner-task-detail/delete-business-partner-task-detail.component';
import {TaskListComponent} from './task-list.component';

describe('TaskListComponent deletion tasks', () => {
  let fixture: ComponentFixture<TaskListComponent>;
  let component: TaskListComponent;
  let getTasks: jest.Mock;
  const task: TaskListItem = {
    id: 'task-1',
    partnerName: {default: 'Acme AG'},
    submittedAt: '2026-10-21T10:00:00Z',
    dueAt: '2026-10-23T10:00:00Z',
    state: TaskStatus.Opened,
    taskType: 'DELETE_BUSINESS_PARTNER',
    allowedActions: new Set([TaskAction.Reject])
  };
  beforeEach(async () => {
    getTasks = jest.fn().mockReturnValue(of({content: [task], page: {totalElements: 1}}));
    await TestBed.configureTestingModule({
      imports: [TaskListComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        {provide: TaskApi, useValue: {getTasks}}
      ]
    }).compileComponents();
    TestBed.inject(TranslateService).use('en');
    fixture = TestBed.createComponent(TaskListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(false);
  });

  it('links deletion tasks to the registered deletion detail page', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('tr[data-cy="task-1"]')).not.toBeNull();
    expect(component.detailRoute(task)).toEqual(['/tasks', 'task-1', 'delete-business-partner']);
    expect(
      routes
        .find(route => route.path === 'tasks')
        ?.children?.find(route => route.path === ':taskId/delete-business-partner')?.component
    ).toBe(DeleteBusinessPartnerTaskDetailComponent);
  });

  it('offers the deletion type and sends it as a backend filter', fakeAsync(() => {
    expect(component.taskTypes).toContain('DELETE_BUSINESS_PARTNER');
    component.filterForm.controls.taskType.setValue(['DELETE_BUSINESS_PARTNER']);
    tick(300);
    expect(getTasks).toHaveBeenLastCalledWith(expect.objectContaining({taskType: ['DELETE_BUSINESS_PARTNER']}));
  }));
});

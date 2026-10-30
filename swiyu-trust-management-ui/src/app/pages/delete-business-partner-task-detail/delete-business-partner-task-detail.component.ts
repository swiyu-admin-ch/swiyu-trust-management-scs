import {DatePipe} from '@angular/common';
import {Component, inject, input, signal, ViewChild} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {RouterLink} from '@angular/router';
import {TranslatePipe} from '@ngx-translate/core';
import {ObAlertComponent, ObButtonDirective, ObDocumentMetaService} from '@oblique/oblique';
import {catchError, EMPTY, startWith, switchMap} from 'rxjs';
import {DeleteBusinessPartnerTask, TaskAction, TaskApi} from '../../api/generated';
import {DomainEventListComponent} from '../../shared/domain-event-list/domain-event-list.component';
import {TaskStatusChipComponent} from '../../shared/task-status-chip/task-status-chip.component';
import {SidepanelService} from '../trust-onboarding-task-detail/sidepanel/sidepanel.service';

@Component({
  selector: 'app-delete-business-partner-task-detail',
  templateUrl: './delete-business-partner-task-detail.component.html',
  imports: [
    DatePipe,
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    ObButtonDirective,
    ObAlertComponent,
    DomainEventListComponent,
    TaskStatusChipComponent
  ]
})
export class DeleteBusinessPartnerTaskDetailComponent {
  private readonly api = inject(TaskApi);
  private readonly sidepanelService = inject(SidepanelService);
  private readonly metaService = inject(ObDocumentMetaService);
  readonly taskId = input.required<string>();
  readonly task = signal<DeleteBusinessPartnerTask | null>(null);
  readonly loadError = signal(false);
  readonly notFound = signal(false);

  @ViewChild(DomainEventListComponent) domainEventList?: DomainEventListComponent;

  constructor() {
    this.metaService.setTitle('task.table.taskType.DELETE_BUSINESS_PARTNER');
    toObservable(this.taskId)
      .pipe(
        switchMap(id =>
          this.sidepanelService.reload$.pipe(
            startWith(undefined),
            switchMap(() => {
              this.task.set(null);
              this.loadError.set(false);
              this.notFound.set(false);
              return this.api.getDeleteBusinessPartnerTask({taskId: id}).pipe(
                catchError(error => {
                  this.notFound.set(error.status === 404);
                  this.loadError.set(true);
                  return EMPTY;
                })
              );
            })
          )
        ),
        takeUntilDestroyed()
      )
      .subscribe(task => {
        this.task.set(task);
        this.domainEventList?.reloadEvents();
      });
  }

  get canReject(): boolean {
    return Array.from(this.task()?.allowedActions ?? []).includes(TaskAction.Reject);
  }

  reject(): void {
    const task = this.task();
    if (task && this.canReject) {
      this.sidepanelService.reject(task);
    }
  }
}

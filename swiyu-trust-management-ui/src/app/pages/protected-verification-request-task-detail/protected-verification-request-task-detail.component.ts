import {DatePipe} from '@angular/common';
import {Component, effect, inject, input, signal, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {TranslatePipe} from '@ngx-translate/core';
import {ObAlertComponent, ObButtonModule, ObColumnLayoutModule, ObDocumentMetaService} from '@oblique/oblique';
import {filter, finalize, take, tap} from 'rxjs';
import {ProtectedVerificationRequestTask, TrustOnboardingTaskAction, TrustOnboardingTaskApi} from '../../api/generated';
import {LocalizeService} from '../../core/i18n/localize.service';
import {LocalizePipe} from '../../core/i18n/localized-text.pipe';
import {DomainEventListComponent} from '../../shared/domain-event-list/domain-event-list.component';
import {TaskStatusChipComponent} from '../../shared/task-status-chip/task-status-chip.component';
import {ZasDataSectionComponent} from './zas-data-section/zas-data-section.component';

@Component({
  selector: 'app-protected-verification-request-task-detail',
  standalone: true,
  templateUrl: './protected-verification-request-task-detail.component.html',
  styleUrl: './protected-verification-request-task-detail.component.scss',
  imports: [
    TranslatePipe,
    ObAlertComponent,
    ObColumnLayoutModule,
    MatButtonModule,
    ObButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    DatePipe,
    DomainEventListComponent,
    TaskStatusChipComponent,
    LocalizePipe,
    ZasDataSectionComponent
  ]
})
export class ProtectedVerificationRequestTaskDetailComponent {
  private readonly api = inject(TrustOnboardingTaskApi);
  private readonly metaService = inject(ObDocumentMetaService);
  private readonly localizeService = inject(LocalizeService);

  TrustOnboardingTaskAction = TrustOnboardingTaskAction;

  taskId = input.required<string>();
  task = signal({} as ProtectedVerificationRequestTask);
  notFoundError = signal(false);
  zasDataOpenedInSession = signal(false);

  showApproveForm = signal(false);
  showRejectForm = signal(false);
  approving = signal(false);
  rejecting = signal(false);
  internalNote = '';
  rejectReason = '';

  @ViewChild(DomainEventListComponent) domainEventList?: DomainEventListComponent;

  constructor() {
    effect(() => {
      this.loadTask();
    });
  }

  isActionAllowed(action: TrustOnboardingTaskAction): boolean {
    return Array.from(this.task()?.allowedActions?.values() || []).includes(action);
  }

  canDecide(): boolean {
    return this.zasDataOpenedInSession() || this.task().zasDataOpened === true;
  }

  onZasDataOpened(): void {
    this.zasDataOpenedInSession.set(true);
    // Refresh so allowedActions reflects the review precondition the backend just persisted - otherwise
    // Approve/Reject stay disabled until the next full page load.
    this.loadTask();
  }

  openApproveForm(): void {
    this.showRejectForm.set(false);
    this.showApproveForm.set(true);
  }

  openRejectForm(): void {
    this.showApproveForm.set(false);
    this.showRejectForm.set(true);
  }

  cancelForms(): void {
    this.showApproveForm.set(false);
    this.showRejectForm.set(false);
    this.internalNote = '';
    this.rejectReason = '';
  }

  approve(): void {
    this.approving.set(true);
    this.api
      .approveProtectedVerificationRequest({
        taskId: this.taskId(),
        request: {internalNote: this.internalNote || undefined}
      })
      .pipe(
        take(1),
        finalize(() => this.approving.set(false))
      )
      .subscribe(() => {
        this.cancelForms();
        this.loadTask();
      });
  }

  reject(): void {
    if (!this.rejectReason.trim()) {
      return;
    }
    this.rejecting.set(true);
    this.api
      .rejectProtectedVerificationRequest({
        taskId: this.taskId(),
        request: {rejectReason: this.rejectReason, internalNote: this.internalNote || undefined}
      })
      .pipe(
        take(1),
        finalize(() => this.rejecting.set(false))
      )
      .subscribe(() => {
        this.cancelForms();
        this.loadTask();
      });
  }

  private loadTask(): void {
    this.api
      .getProtectedVerificationRequestTask({taskId: this.taskId()})
      .pipe(
        filter(task => task != null),
        tap(task => {
          this.task.set(task);
          this.notFoundError.set(false);
          this.metaService.setTitle(
            this.localizeService.localize(task.partnerName) || 'Protected Verification Request Task'
          );
        }),
        finalize(() => {
          if (this.domainEventList) {
            this.domainEventList.reloadEvents();
          }
        })
      )
      .subscribe({
        error: err => {
          this.task.set({} as ProtectedVerificationRequestTask);
          if (err.status === 404) {
            this.notFoundError.set(true);
          }
        }
      });
  }
}

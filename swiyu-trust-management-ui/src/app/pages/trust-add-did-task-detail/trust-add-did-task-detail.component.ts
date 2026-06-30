import {DatePipe} from '@angular/common';
import {Component, effect, inject, input, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';
import {ObAlertComponent, ObButtonModule, ObColumnLayoutModule, ObDocumentMetaService} from '@oblique/oblique';
import {filter, tap} from 'rxjs';
import {TrustAddDidTask, TrustOnboardingTaskApi} from '../../api/generated';
import {LocalizeService} from '../../core/i18n/localize.service';
import {LocalizePipe} from '../../core/i18n/localized-text.pipe';
import {DomainEventListComponent} from '../../shared/domain-event-list/domain-event-list.component';
import {TaskStatusChipComponent} from '../../shared/task-status-chip/task-status-chip.component';

@Component({
  selector: 'app-trust-add-did-task-detail',
  standalone: true,
  templateUrl: './trust-add-did-task-detail.component.html',
  styleUrl: './trust-add-did-task-detail.component.scss',
  imports: [
    TranslatePipe,
    ObAlertComponent,
    ObColumnLayoutModule,
    MatButtonModule,
    ObButtonModule,
    MatIconModule,
    DatePipe,
    DomainEventListComponent,
    TaskStatusChipComponent,
    LocalizePipe
  ]
})
export class TrustAddDidTaskDetailComponent {
  private readonly api = inject(TrustOnboardingTaskApi);
  private readonly metaService = inject(ObDocumentMetaService);
  private readonly localizeService = inject(LocalizeService);

  taskId = input.required<string>();
  task = signal({} as TrustAddDidTask);
  notFoundError = signal(false);

  constructor() {
    effect(() => {
      this.loadTask();
    });
  }

  private loadTask() {
    this.api
      .getAddDidTask({taskId: this.taskId()})
      .pipe(
        filter(task => task != null),
        tap(task => {
          this.task.set(task);
          this.notFoundError.set(false);
          this.metaService.setTitle(this.localizeService.localize(task.partnerName) || 'Add DID Task');
        })
      )
      .subscribe({
        error: err => {
          this.task.set({} as TrustAddDidTask);
          if (err.status === 404) {
            this.notFoundError.set(true);
          }
        }
      });
  }
}

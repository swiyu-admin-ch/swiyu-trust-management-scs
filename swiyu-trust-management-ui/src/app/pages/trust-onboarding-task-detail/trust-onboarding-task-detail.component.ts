import {Clipboard} from '@angular/cdk/clipboard';
import {DatePipe, KeyValuePipe} from '@angular/common';
import {Component, effect, inject, input, signal, ViewChild} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatSelectModule} from '@angular/material/select';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {
  ObAlertComponent,
  ObButtonModule,
  ObColumnLayoutModule,
  ObDocumentMetaService,
  ObENotificationType,
  ObNotificationService
} from '@oblique/oblique';
import {RemoveWhitespacePipe} from '../../core/format/remove-whitespace.pipe';
import {DomainEventListComponent} from '../../shared/domain-event-list/domain-event-list.component';
import {SidepanelService} from './sidepanel/sidepanel.service';

import {HttpContext} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatPaginator, MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {catchError, filter, finalize, mergeMap, take, tap} from 'rxjs';
import {
  Language,
  PageMetadata,
  TrustOnboardingDocumentApi,
  TrustOnboardingSubmissionDocumentListItemDto,
  TrustOnboardingTask,
  TrustOnboardingTaskAction,
  TrustOnboardingTaskApi,
  TrustOnboardingTaskStatus
} from '../../api/generated';
import {ConcatI18nKey} from '../../core/format/concat-i18n-key';
import {TaskStatusChipComponent} from '../../shared/task-status-chip/task-status-chip.component';

@Component({
  selector: 'app-trust-onboarding-task-detail',
  imports: [
    TranslatePipe,
    ObAlertComponent,
    ObColumnLayoutModule,
    MatButtonModule,
    ObButtonModule,
    MatIconModule,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    MatFormFieldModule,
    MatSelectModule,
    FormsModule,
    DatePipe,
    KeyValuePipe,
    MatCardModule,
    RemoveWhitespacePipe,
    MatTableModule,
    MatPaginatorModule,
    DomainEventListComponent,
    TaskStatusChipComponent,
    ConcatI18nKey
  ],
  templateUrl: './trust-onboarding-task-detail.component.html',
  styleUrl: './trust-onboarding-task-detail.component.scss'
})
export class TrustOnboardingTaskDetailComponent {
  private readonly api = inject(TrustOnboardingTaskApi);
  private readonly documentsApi = inject(TrustOnboardingDocumentApi);
  private readonly notificationService = inject(ObNotificationService);
  private readonly translateService = inject(TranslateService);
  private readonly clipboard = inject(Clipboard);
  readonly sidepanelService = inject(SidepanelService);
  private readonly metaService = inject(ObDocumentMetaService);

  @ViewChild(DomainEventListComponent) domainEventList!: DomainEventListComponent;
  @ViewChild(MatPaginator) documentsPaginator!: MatPaginator;

  documentsPageable = signal({
    size: 5,
    number: 0,
    totalElements: 0,
    totalPages: 0
  } as PageMetadata);
  documents = new MatTableDataSource<TrustOnboardingSubmissionDocumentListItemDto>();
  documentDisplayedColumns = ['name', 'submittedAt', 'download-link'];

  taskId = input.required<string>();
  task = signal({} as TrustOnboardingTask);
  notFoundError = signal(false);

  protected readonly TrustOnboardingTaskStatus = TrustOnboardingTaskStatus;
  protected readonly Language = Language;
  protected readonly TrustOnboardingTaskAction = TrustOnboardingTaskAction;

  constructor() {
    this.sidepanelService.reload$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.loadTask();
    });
    this.translateSetup();

    effect(() => {
      this.documents.paginator = this.documentsPaginator;
      this.loadTask();
    });
  }

  shareLink() {
    const link = window.location.toString();
    this.translateService.get('app.trust-onboarding-task.actions.share-link.notification.title').subscribe(title => {
      this.translateService
        .get('app.trust-onboarding-task.actions.share-link.notification.message', {link: link})
        .subscribe(message => {
          this.notificationService.send({title: title, message}, ObENotificationType.INFO);
        });
    });
    this.clipboard.copy(link);
  }

  isActionAllowed(action: TrustOnboardingTaskAction) {
    return Array.from(this.task().allowedActions.values() || []).includes(action);
  }

  assignSelf(task: TrustOnboardingTask) {
    this.api
      .assignSelf({
        taskId: task.id
      })
      .pipe(take(1))
      .subscribe(() => {
        this.loadTask();
      });
  }

  loadDocuments() {
    return this.documentsApi
      .getTrustOnboardingSubmissionDocuments({
        taskId: this.taskId(),
        page: this.documentsPageable().number,
        size: this.documentsPageable().size,
        sort: ['createdAt,desc']
      })
      .pipe(
        tap(documents => {
          this.documents.data = documents.content ?? [];
          if (documents.page) this.documentsPageable.set(documents.page);
        }),
        catchError(err => {
          this.documents.data = [];
          return err;
        })
      );
  }

  protected downloadDocument(documentListItem: TrustOnboardingSubmissionDocumentListItemDto) {
    this.documentsApi
      .getTrustOnboardingSubmissionDocument(
        {
          taskId: this.taskId(),
          documentId: documentListItem.id!
        },
        'body',
        false,
        {
          httpHeaderAccept: 'application/octet-stream',
          context: new HttpContext(),
          transferCache: false
        }
      )
      .subscribe({
        next: download => {
          const blob = new Blob([download], {type: documentListItem.mediaType});
          const url = window.URL.createObjectURL(blob);

          const a = document.createElement('a');
          a.href = url;
          a.download = documentListItem.name!;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
        }
      });
  }

  protected reloadDocuments(event: PageEvent) {
    this.documentsPageable.update(value => {
      value.number = event.pageIndex;
      value.size = event.pageSize;
      return value;
    });
    this.loadDocuments().subscribe();
  }

  private loadTask() {
    this.api
      .getTask({taskId: this.taskId()})
      .pipe(
        filter(task => task != null),
        tap(task => {
          this.task.set(task);
          this.notFoundError.set(false);
        }),
        tap(task => {
          this.metaService.setTitle(task.entityNameDefault);
        }),
        mergeMap(() => {
          return this.loadDocuments();
        }),
        finalize(() => {
          if (this.domainEventList) {
            // might not be initialized yet
            this.domainEventList.reloadEvents();
          }
        })
      )
      .subscribe({
        error: err => {
          this.task.set({} as TrustOnboardingTask);
          if (err.status === 404) {
            this.notFoundError.set(true);
          }
        }
      });
  }

  private translateSetup() {
    // Required for translate service auto collection of i18n keys
    // @see: NameLanguage
    this.translateService.get('app.trust-onboarding-task.fields.name.en.label');
    this.translateService.get('app.trust-onboarding-task.fields.name.de-CH.label');
    this.translateService.get('app.trust-onboarding-task.fields.name.fr-CH.label');
    this.translateService.get('app.trust-onboarding-task.fields.name.it-CH.label');
    this.translateService.get('app.trust-onboarding-task.fields.name.rm-CH.label');
    // @see: CorrespondanceLanguage
    this.translateService.get('app.trust-onboarding-task.fields.correspondance_language.value.en');
    this.translateService.get('app.trust-onboarding-task.fields.correspondance_language.value.de-CH');
    this.translateService.get('app.trust-onboarding-task.fields.correspondance_language.value.fr-CH');
    this.translateService.get('app.trust-onboarding-task.fields.correspondance_language.value.it-CH');
    this.translateService.get('app.trust-onboarding-task.fields.correspondance_language.value.rm-CH');

    // @see: TrustOnboardingTaskContactType
    this.translateService.get('app.trust-onboarding-task.actions.contacts.type.CONTACT_PERSON.value');
    this.translateService.get('app.trust-onboarding-task.actions.contacts.type.AUTHORISED_SIGNATORY.value');

    // @see: PartnerType
    this.translateService.get('app.trust-onboarding-task.fields.partnerType.value.GOVERNMENTAL_INSTITUTION');
    this.translateService.get('app.trust-onboarding-task.fields.partnerType.value.BUSINESS');
    this.translateService.get('app.trust-onboarding-task.fields.partnerType.value.INDIVIDUAL');
    this.translateService.get('app.trust-onboarding-task.fields.partnerType.value.UNKNOWN');

    // boolean
    this.translateService.get('app.trust-onboarding-task.fields.isRegisteredInCommercialRegister.value.true');
    this.translateService.get('app.trust-onboarding-task.fields.isRegisteredInCommercialRegister.value.false');
  }
}

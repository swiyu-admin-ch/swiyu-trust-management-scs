import {DatePipe} from '@angular/common';
import {AfterViewInit, Component, ViewChild, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatNativeDateModule} from '@angular/material/core';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatMenuModule} from '@angular/material/menu';
import {MatPaginator, MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatSelectModule} from '@angular/material/select';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {RouterLink} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {ObButtonDirective, ObDatepickerModule, ObDocumentMetaService} from '@oblique/oblique';
import {take} from 'rxjs';
import {
  TrustOnboardingTaskAction,
  TrustOnboardingTaskApi,
  TrustOnboardingTaskListItem,
  TrustOnboardingTaskStatus
} from '../../api/generated';
import {MultiLanguageTextPipe} from '../../core/i18n/multi-language-text.pipe';
import {TaskStatusChipComponent} from '../../shared/task-status-chip/task-status-chip.component';
import {SidepanelService} from '../trust-onboarding-task-detail/sidepanel/sidepanel.service';

type DateRangeGroup = FormGroup<{
  start: FormControl<Date | null>;
  end: FormControl<Date | null>;
}>;

interface FilterFormModel {
  assignee: FormControl<string>;
  state: FormControl<string[]>;
  submittedAt: DateRangeGroup;
  dueAt: DateRangeGroup;
}

@Component({
  selector: 'app-task-list',
  standalone: true,
  templateUrl: './trust-onboarding-task-list.component.html',
  styleUrls: ['./trust-onboarding-task-list.scss'],
  imports: [
    MatTableModule,
    MatPaginatorModule,
    DatePipe,
    TranslateModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    ObButtonDirective,
    ObDatepickerModule,
    MatMenuModule,
    MatIconModule,
    MultiLanguageTextPipe,
    TaskStatusChipComponent,
    RouterLink
  ]
})
export class TrustOnboardingTaskListComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(TrustOnboardingTaskApi);
  private readonly meta = inject(ObDocumentMetaService);
  readonly sidepanelService = inject(SidepanelService);

  TrustOnboardingTaskAction = TrustOnboardingTaskAction;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  readonly filterForm: FormGroup<FilterFormModel> = this.fb.group({
    assignee: this.fb.nonNullable.control(''),
    state: this.fb.nonNullable.control<string[]>([]),
    submittedAt: this.buildDateRange(),
    dueAt: this.buildDateRange()
  });
  displayedColumns = ['issuerOrVerifier', 'taskType', 'submittedAt', 'dueAt', 'state', 'assignee', 'actions'];
  dataSource = new MatTableDataSource<TrustOnboardingTaskListItem>();
  pageSize = 10;
  pageIndex = 0;
  totalItems = 0;
  statuses = Object.values(TrustOnboardingTaskStatus);

  constructor() {
    this.meta.setTitle('app.menu.tasks');
    this.sidepanelService.reload$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.loadTasks();
    });
  }

  ngAfterViewInit(): void {
    this.paginator.page.subscribe((event: PageEvent) => {
      this.pageIndex = event.pageIndex;
      this.pageSize = event.pageSize;
      this.loadTasks();
    });
    this.loadTasks();
  }

  applyFilter(): void {
    this.pageIndex = 0;
    this.loadTasks();
  }

  resetFilter(): void {
    this.filterForm.reset();
    this.pageIndex = 0;
    this.loadTasks();
  }

  isActionAllowed(action: TrustOnboardingTaskAction, task: TrustOnboardingTaskListItem) {
    return Array.from(task?.allowedActions.values() || []).includes(action);
  }

  assignSelf(task: TrustOnboardingTaskListItem) {
    this.api
      .assignSelf({
        taskId: task.id
      })
      .pipe(take(1))
      .subscribe(() => {
        this.loadTasks();
      });
  }

  private loadTasks(): void {
    const {assignee, state, submittedAt, dueAt} = this.filterForm.value;

    const formatter = new Intl.DateTimeFormat('en-CA'); // date format YYYY-MM-DD

    this.api
      .getTasks({
        size: this.pageSize,
        page: this.pageIndex,
        sort: ['submittedAt,desc', 'partnerName,desc'],
        assignee: assignee?.trim() || undefined,
        state: state?.length ? state : undefined,
        submissionStartDate: submittedAt?.start ? formatter.format(submittedAt.start) : undefined,
        submissionEndDate: submittedAt?.end ? formatter.format(submittedAt.end) : undefined,
        dueStartDate: dueAt?.start ? formatter.format(dueAt.start) : undefined,
        dueEndDate: dueAt?.end ? formatter.format(dueAt.end) : undefined
      })
      .subscribe({
        next: response => {
          this.dataSource.data = response.content ?? [];
          this.totalItems = response.page?.totalElements ?? 0;
        }
      });
  }

  private buildDateRange(): DateRangeGroup {
    return this.fb.group({
      start: this.fb.control<Date | null>(null),
      end: this.fb.control<Date | null>(null)
    });
  }
}

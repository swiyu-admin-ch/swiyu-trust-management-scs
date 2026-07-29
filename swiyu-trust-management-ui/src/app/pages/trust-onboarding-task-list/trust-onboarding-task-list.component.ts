import {DatePipe} from '@angular/common';
import {AfterViewInit, Component, inject, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
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
import {debounceTime, take} from 'rxjs';
import {
  TrustOnboardingTaskAction,
  TrustOnboardingTaskApi,
  TrustOnboardingTaskListItem,
  TrustOnboardingTaskStatus
} from '../../api/generated';
import {LocalizePipe} from '../../core/i18n/localized-text.pipe';
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
  overdue: FormControl<boolean>;
  taskType: FormControl<string[]>;
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
    MatCheckboxModule,
    ObButtonDirective,
    ObDatepickerModule,
    MatMenuModule,
    MatIconModule,
    LocalizePipe,
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
    overdue: this.fb.nonNullable.control(false),
    taskType: this.fb.nonNullable.control<string[]>([])
  });
  displayedColumns = ['issuerOrVerifier', 'taskType', 'submittedAt', 'dueAt', 'assignee', 'state', 'actions'];
  dataSource = new MatTableDataSource<TrustOnboardingTaskListItem>();
  pageSize = 10;
  pageIndex = 0;
  totalItems = 0;
  statuses = Object.values(TrustOnboardingTaskStatus);
  taskTypes = ['ONBOARDING', 'ADD_DID'];

  constructor() {
    this.meta.setTitle('app.menu.tasks');
    this.sidepanelService.reload$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.loadTasks();
    });
    this.filterForm.valueChanges.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => {
      this.pageIndex = 0;
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

  resetFilter(): void {
    this.filterForm.reset();
  }

  isActionAllowed(action: TrustOnboardingTaskAction, task: TrustOnboardingTaskListItem) {
    return Array.from(task?.allowedActions.values() || []).includes(action);
  }

  isOverdue(task: TrustOnboardingTaskListItem): boolean {
    return !!task.dueAt && new Date(task.dueAt) < new Date();
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
    const {assignee, state, submittedAt, overdue, taskType} = this.filterForm.value;

    const formatter = new Intl.DateTimeFormat('en-CA'); // date format YYYY-MM-DD
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);

    this.api
      .getTasks({
        size: this.pageSize,
        page: this.pageIndex,
        sort: ['submittedAt,desc', 'partnerName,desc'],
        assignee: assignee?.trim() || undefined,
        state: state?.length ? state : undefined,
        submissionStartDate: submittedAt?.start ? formatter.format(submittedAt.start) : undefined,
        submissionEndDate: submittedAt?.end ? formatter.format(submittedAt.end) : undefined,
        dueEndDate: overdue ? formatter.format(yesterday) : undefined,
        taskType: taskType?.length ? taskType : undefined
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

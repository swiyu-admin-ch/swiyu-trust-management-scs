import {DatePipe} from '@angular/common';
import {AfterViewInit, Component, DestroyRef, effect, inject, input, signal, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ReactiveFormsModule} from '@angular/forms';
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
import {TranslateModule} from '@ngx-translate/core';
import {ObDatepickerModule} from '@oblique/oblique';
import {DomainEventLog, DomainEventLogApi} from '../../api/generated';

@Component({
  selector: 'app-domain-event-list',
  standalone: true,
  templateUrl: './domain-event-list.component.html',
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
    ObDatepickerModule,
    MatMenuModule,
    MatIconModule
  ]
})
export class DomainEventListComponent implements AfterViewInit {
  private readonly api = inject(DomainEventLogApi);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  displayedColumns = ['triggeredAt', 'triggeredBy', 'eventType', 'partnerNote', 'internalNote'];
  dataSource = new MatTableDataSource<DomainEventLog>();
  private reloadTrigger = signal(0);
  trustOnboardingTaskId = input.required<string>();
  pageSize = signal(10);
  pageIndex = signal(0);
  totalItems = signal(0);
  loading = signal(false);
  error = signal<string | null>(null);

  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const id = this.trustOnboardingTaskId();
      const page = this.pageIndex();
      const size = this.pageSize();
      this.reloadTrigger(); // so we can trigger a reload from outside

      if (!id) {
        return;
      }

      this.loading.set(true);
      this.error.set(null);

      this.api
        .getDomainEventLogs({
          page,
          size,
          trustOnboardingTaskId: id
        })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: response => {
            this.dataSource.data = response.content ?? [];
            this.totalItems.set(response.page?.totalElements ?? 0);
            this.loading.set(false);
          }
        });
    });
  }

  ngAfterViewInit(): void {
    this.paginator.page.subscribe((event: PageEvent) => {
      this.pageIndex.set(event.pageIndex);
      this.pageSize.set(event.pageSize);
    });
  }

  public reloadEvents() {
    this.reloadTrigger.set(this.reloadTrigger() + 1);
  }
}

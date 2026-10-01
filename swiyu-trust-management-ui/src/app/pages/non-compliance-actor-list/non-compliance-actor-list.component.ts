import {AfterViewInit, Component, inject, signal, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatMenuModule} from '@angular/material/menu';
import {MatPaginator, MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {RouterLink} from '@angular/router';
import {_, TranslateModule, TranslateService} from '@ngx-translate/core';
import {ObButtonDirective, ObDocumentMetaService, ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {catchError, debounceTime, EMPTY, finalize, merge, Observable, Subject, switchMap, take, tap} from 'rxjs';
import {NonCompliantActor, NonCompliantActorApi} from '../../api/generated';
import {ConfirmDialogComponent} from './confirm-dialog.component';
import {NonComplianceReasonPipe} from './non-compliance-reason.pipe';

interface FilterFormModel {
  did: FormControl<string>;
  businessPartnerId: FormControl<string>;
}

@Component({
  selector: 'app-non-compliance-actor-list',
  standalone: true,
  templateUrl: './non-compliance-actor-list.component.html',
  styleUrls: ['./non-compliance-actor-list.component.scss'],
  imports: [
    MatTableModule,
    MatPaginatorModule,
    TranslateModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDialogModule,
    ObButtonDirective,
    RouterLink,
    NonComplianceReasonPipe
  ]
})
export class NonComplianceActorListComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(NonCompliantActorApi);
  private readonly meta = inject(ObDocumentMetaService);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(ObNotificationService);
  private readonly dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  readonly filterForm: FormGroup<FilterFormModel> = this.fb.group({
    did: this.fb.nonNullable.control(''),
    businessPartnerId: this.fb.nonNullable.control('')
  });
  displayedColumns = ['did', 'businessPartnerId', 'reason', 'actions'];
  dataSource = new MatTableDataSource<NonCompliantActor>();
  pageSize = 10;
  pageIndex = 0;
  totalItems = 0;
  readonly loading = signal(false);

  private readonly paginatorPage$ = new Subject<PageEvent>();
  private readonly reloadTrigger$ = new Subject<void>();

  constructor() {
    this.meta.setTitle('app.menu.nonComplianceActors');
    merge(
      this.filterForm.valueChanges.pipe(
        debounceTime(300),
        tap(() => (this.pageIndex = 0))
      ),
      this.paginatorPage$.pipe(
        tap((event: PageEvent) => {
          this.pageIndex = event.pageIndex;
          this.pageSize = event.pageSize;
        })
      ),
      this.reloadTrigger$
    )
      .pipe(
        takeUntilDestroyed(),
        switchMap(() => this.loadActors())
      )
      .subscribe();
  }

  ngAfterViewInit(): void {
    this.paginator.page.subscribe(event => this.paginatorPage$.next(event));
    this.reloadTrigger$.next();
  }

  detailRoute(actor: NonCompliantActor): string[] {
    return ['/non-compliance-actors', actor.id];
  }

  resetFilter(): void {
    this.filterForm.reset();
  }

  delete(actor: NonCompliantActor): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {did: actor.did, businessPartnerId: actor.businessPartnerId}
    });
    ref
      .afterClosed()
      .pipe(take(1))
      .subscribe(confirmed => {
        if (!confirmed) {
          return;
        }
        this.api
          .deleteNonCompliantActor({nonCompliantActorId: actor.id})
          .pipe(take(1))
          .subscribe({
            next: () => {
              this.notify(_('nonCompliance.list.notification.delete.success'), ObENotificationType.INFO);
              if (this.dataSource.data.length === 1 && this.pageIndex > 0) {
                this.pageIndex--;
              }
              this.reloadTrigger$.next();
            },
            error: () => this.notify(_('nonCompliance.list.notification.delete.error'), ObENotificationType.ERROR)
          });
      });
  }

  private loadActors(): Observable<unknown> {
    const {did, businessPartnerId} = this.filterForm.value;
    this.loading.set(true);
    return this.api
      .getNonCompliantActors({
        size: this.pageSize,
        page: this.pageIndex,
        sort: ['did,asc'],
        did: did?.trim() || undefined,
        businessPartnerId: businessPartnerId?.trim() || undefined
      })
      .pipe(
        catchError(() => {
          this.notify(_('nonCompliance.list.notification.load.error'), ObENotificationType.ERROR);
          return EMPTY;
        }),
        tap(response => {
          this.dataSource.data = response.content ?? [];
          this.totalItems = response.page?.totalElements ?? 0;
        }),
        finalize(() => this.loading.set(false))
      );
  }

  private notify(messageKey: string, type: ObENotificationType): void {
    this.translateService
      .get(messageKey)
      .pipe(take(1))
      .subscribe(message => this.notificationService.send({message}, type));
  }
}

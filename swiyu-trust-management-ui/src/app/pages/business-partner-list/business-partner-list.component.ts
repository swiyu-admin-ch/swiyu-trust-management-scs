import {DatePipe} from '@angular/common';
import {Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatMenuModule} from '@angular/material/menu';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatSortModule, Sort, SortDirection} from '@angular/material/sort';
import {MatTableDataSource, MatTableModule} from '@angular/material/table';
import {ActivatedRoute, ParamMap, Params, Router} from '@angular/router';
import {_, TranslateModule, TranslateService} from '@ngx-translate/core';
import {ObButtonDirective, ObDocumentMetaService, ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {catchError, debounceTime, EMPTY, finalize, Observable, switchMap, take, tap} from 'rxjs';
import {BusinessPartnerViewApi, BusinessPartnerViewListItem} from '../../api/generated';
import {AppConfigService} from '../../core/appconfig/app-config.service';
import {BusinessPartnerDeletePanelService} from './delete-panel/business-partner-delete-panel.service';

interface FilterFormModel {
  search: FormControl<string>;
}

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];
const DEFAULT_SORT_COLUMN = 'name';

/**
 * Status to translation key. Wrapped in the `_()` marker so `npm run generate:translations` keeps these keys: the
 * extractor cannot see a key that is only ever built as a template string, and `--clean` would drop it.
 */
const STATUS_TRANSLATION_KEY: Record<BusinessPartnerViewListItem.StatusEnum | 'NOT_ONBOARDED', string> = {
  ACTIVE: _('businessPartner.list.status.ACTIVE'),
  DEACTIVATED: _('businessPartner.list.status.DEACTIVATED'),
  NOT_ONBOARDED: _('businessPartner.list.status.NOT_ONBOARDED')
};

const SORT_PROPERTY_BY_COLUMN: Record<string, string> = {
  businessPartnerId: 'businessPartnerId',
  name: 'partnerName',
  status: 'status',
  validUntil: 'validUntil'
};

@Component({
  selector: 'app-business-partner-list',
  standalone: true,
  templateUrl: './business-partner-list.component.html',
  styleUrls: ['./business-partner-list.component.scss'],
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    DatePipe,
    TranslateModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    ObButtonDirective
  ]
})
export class BusinessPartnerListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(BusinessPartnerViewApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly meta = inject(ObDocumentMetaService);
  private readonly appConfigService = inject(AppConfigService);
  private readonly deletePanelService = inject(BusinessPartnerDeletePanelService);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(ObNotificationService);

  readonly filterForm: FormGroup<FilterFormModel> = this.fb.group({
    search: this.fb.nonNullable.control('')
  });
  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  readonly loading = signal(false);
  displayedColumns = ['businessPartnerId', 'name', 'status', 'validUntil', 'actions'];
  dataSource = new MatTableDataSource<BusinessPartnerViewListItem>();
  pageSize = DEFAULT_PAGE_SIZE;
  pageIndex = 0;
  totalItems = 0;
  sortActive = DEFAULT_SORT_COLUMN;
  sortDirection: SortDirection = 'asc';

  constructor() {
    this.meta.setTitle('app.menu.businessPartners');

    // The url holds the whole list state, so every interaction only navigates and the reload follows the url change.
    this.route.queryParamMap
      .pipe(
        takeUntilDestroyed(),
        switchMap(params => this.applyStateAndLoad(params))
      )
      .subscribe();

    this.filterForm.controls.search.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed())
      .subscribe(search => this.writeStateToUrl({search: search.trim() || null, page: null}, true));

    // A created deletion request changes nothing in the url, so the list has to be told to reload.
    this.deletePanelService.created$
      .pipe(
        takeUntilDestroyed(),
        switchMap(() => this.loadPartners(this.filterForm.controls.search.value.trim()))
      )
      .subscribe();
  }

  onPage(event: PageEvent): void {
    this.writeStateToUrl({
      page: event.pageIndex === 0 ? null : event.pageIndex,
      size: event.pageSize === DEFAULT_PAGE_SIZE ? null : event.pageSize
    });
  }

  onSortChange(sort: Sort): void {
    const isDefault = sort.active === DEFAULT_SORT_COLUMN && sort.direction === 'asc';
    this.writeStateToUrl({
      sort: !sort.direction || isDefault ? null : `${sort.active},${sort.direction}`,
      page: null
    });
  }

  resetFilter(): void {
    this.filterForm.controls.search.setValue('');
  }

  statusKey(partner: BusinessPartnerViewListItem): string {
    return STATUS_TRANSLATION_KEY[partner.status ?? 'NOT_ONBOARDED'];
  }

  /** Feature toggle EIDARTFE_1571: without it the delete entry must not appear at all. */
  get deletionAvailable(): boolean {
    return this.appConfigService.featureToggles.EIDARTFE_1571_COMPLETE_BP_OFFBOARDING;
  }

  requestDeletion(partner: BusinessPartnerViewListItem): void {
    this.deletePanelService.open(partner);
  }

  copyPartnerId(partner: BusinessPartnerViewListItem): void {
    navigator.clipboard
      .writeText(partner.businessPartnerId ?? '')
      .then(() => this.notify(_('businessPartner.list.notification.copyId.success'), ObENotificationType.INFO))
      .catch(() => this.notify(_('businessPartner.list.notification.copyId.error'), ObENotificationType.ERROR));
  }

  private applyStateAndLoad(params: ParamMap): Observable<unknown> {
    this.pageIndex = this.readPageIndex(params);
    this.pageSize = this.readPageSize(params);
    this.readSort(params);

    const search = params.get('search') ?? '';
    if (this.filterForm.controls.search.value !== search) {
      // Only mirrors the url into the input, so it must not start another navigation.
      this.filterForm.controls.search.setValue(search, {emitEvent: false});
    }
    return this.loadPartners(search);
  }

  private readPageIndex(params: ParamMap): number {
    const page = Number(params.get('page'));
    return Number.isInteger(page) && page > 0 ? page : 0;
  }

  private readPageSize(params: ParamMap): number {
    const size = Number(params.get('size'));
    return PAGE_SIZE_OPTIONS.includes(size) ? size : DEFAULT_PAGE_SIZE;
  }

  private readSort(params: ParamMap): void {
    const [column, direction] = (params.get('sort') ?? '').split(',');
    // The url is editable by hand, and an unknown sort property would make the backend answer with an error.
    this.sortActive = Object.hasOwn(SORT_PROPERTY_BY_COLUMN, column) ? column : DEFAULT_SORT_COLUMN;
    this.sortDirection = direction === 'desc' ? 'desc' : 'asc';
  }

  private writeStateToUrl(queryParams: Params, replaceUrl = false): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl
    });
  }

  private loadPartners(search: string): Observable<unknown> {
    this.loading.set(true);
    return this.api
      .getBusinessPartnerViews({
        page: this.pageIndex,
        size: this.pageSize,
        sort: [`${SORT_PROPERTY_BY_COLUMN[this.sortActive]},${this.sortDirection}`],
        searchText: search || undefined
      })
      .pipe(
        catchError(() => {
          this.notify(_('businessPartner.list.notification.load.error'), ObENotificationType.ERROR);
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

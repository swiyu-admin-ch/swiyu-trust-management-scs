import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {PageEvent} from '@angular/material/paginator';
import {ActivatedRoute, convertToParamMap, ParamMap, Params, provideRouter, Router} from '@angular/router';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {BehaviorSubject, of, throwError} from 'rxjs';
import {BusinessPartnerViewApi} from '../../api/generated';
import {BusinessPartnerListComponent} from './business-partner-list.component';

describe('BusinessPartnerListComponent', () => {
  let component: BusinessPartnerListComponent;
  let fixture: ComponentFixture<BusinessPartnerListComponent>;
  let mockApi: jest.Mocked<BusinessPartnerViewApi>;
  let mockNotificationService: jest.Mocked<ObNotificationService>;
  let navigateSpy: jest.SpiedFunction<Router['navigate']>;
  let queryParamMap$: BehaviorSubject<ParamMap>;

  const partner = {
    businessPartnerId: 'b2a2c5b6-0000-0000-0000-000000000001',
    name: 'Acme AG',
    status: 'ACTIVE' as const,
    validUntil: '2029-03-31T15:00:00Z'
  };

  const lastApiCall = (): Record<string, unknown> =>
    mockApi.getBusinessPartnerViews.mock.calls.at(-1)![0] as Record<string, unknown>;

  const lastNavigation = (): Params => navigateSpy.mock.calls.at(-1)![1]!.queryParams as Params;

  const navigateTo = (params: Params): void => queryParamMap$.next(convertToParamMap(params));

  beforeEach(async () => {
    mockApi = {
      getBusinessPartnerViews: jest.fn().mockReturnValue(of({content: [partner], page: {totalElements: 1}}))
    } as unknown as jest.Mocked<BusinessPartnerViewApi>;

    mockNotificationService = {send: jest.fn()} as unknown as jest.Mocked<ObNotificationService>;
    queryParamMap$ = new BehaviorSubject<ParamMap>(convertToParamMap({}));

    await TestBed.configureTestingModule({
      imports: [BusinessPartnerListComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        {provide: BusinessPartnerViewApi, useValue: mockApi},
        {provide: ObNotificationService, useValue: mockNotificationService},
        {provide: ActivatedRoute, useValue: {queryParamMap: queryParamMap$.asObservable()}}
      ]
    }).compileComponents();

    TestBed.inject(TranslateService).use('en');
    // The component only states its intent in the url; the router's behaviour is not under test here.
    navigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(BusinessPartnerListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(false);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the first page sorted by name when the url carries no state', () => {
    expect(lastApiCall()).toEqual({page: 0, size: 10, sort: ['partnerName,asc'], searchText: undefined});
    expect(component.dataSource.data).toHaveLength(1);
    expect(component.totalItems).toBe(1);
  });

  it('takes page, size, sorting and search from the url', () => {
    navigateTo({page: '2', size: '25', sort: 'status,desc', search: 'acme'});

    expect(lastApiCall()).toEqual({page: 2, size: 25, sort: ['status,desc'], searchText: 'acme'});
    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(25);
    expect(component.sortActive).toBe('status');
    expect(component.sortDirection).toBe('desc');
    expect(component.filterForm.controls.search.value).toBe('acme');
  });

  it('falls back to the default sorting for a sort property the backend does not know', () => {
    navigateTo({sort: 'dropTable,desc'});

    expect(lastApiCall()['sort']).toEqual(['partnerName,desc']);
    expect(component.sortActive).toBe('name');
  });

  it('falls back to the default page size for a size that is not offered', () => {
    navigateTo({size: '1000', page: '-1'});

    expect(lastApiCall()).toMatchObject({page: 0, size: 10});
  });

  it('writes the search text into the url after the debounce and goes back to the first page', fakeAsync(() => {
    component.filterForm.controls.search.setValue('  acme  ');
    tick(300);

    expect(lastNavigation()).toEqual({search: 'acme', page: null});
  }));

  it('does not navigate while mirroring the url into the search input', () => {
    navigateTo({search: 'acme'});

    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('writes a page change into the url', () => {
    component.onPage({pageIndex: 3, pageSize: 25, length: 100} as PageEvent);

    expect(lastNavigation()).toEqual({page: 3, size: 25});
  });

  it('drops page and size from the url when they are back at their default', () => {
    component.onPage({pageIndex: 0, pageSize: 10, length: 100} as PageEvent);

    expect(lastNavigation()).toEqual({page: null, size: null});
  });

  it('writes a sort change into the url', () => {
    component.onSortChange({active: 'validUntil', direction: 'desc'});

    expect(lastNavigation()).toEqual({sort: 'validUntil,desc', page: null});
  });

  it('drops the sorting from the url when it is back at its default', () => {
    component.onSortChange({active: 'name', direction: 'asc'});

    expect(lastNavigation()).toEqual({sort: null, page: null});
  });

  it('labels a partner without an identity as not onboarded', () => {
    expect(component.statusKey({businessPartnerId: partner.businessPartnerId})).toBe(
      'businessPartner.list.status.NOT_ONBOARDED'
    );
    expect(component.statusKey(partner)).toBe('businessPartner.list.status.ACTIVE');
  });

  it('shows the empty state when there are no results', () => {
    mockApi.getBusinessPartnerViews.mockReturnValue(of({content: [], page: {totalElements: 0}}));

    navigateTo({search: 'nothing'});
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-cy="noDataRow"]')).not.toBeNull();
  });

  it('notifies the user when the partners cannot be loaded', () => {
    mockApi.getBusinessPartnerViews.mockReturnValue(throwError(() => new Error('boom')));

    navigateTo({page: '1'});

    expect(mockNotificationService.send).toHaveBeenCalledWith(
      {message: 'businessPartner.list.notification.load.error'},
      ObENotificationType.ERROR
    );
    expect(component.loading()).toBe(false);
  });

  it('copies the partner id to the clipboard and confirms it', fakeAsync(() => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {value: {writeText}, configurable: true});

    component.copyPartnerId(partner);
    tick();

    expect(writeText).toHaveBeenCalledWith(partner.businessPartnerId);
    expect(mockNotificationService.send).toHaveBeenCalledWith(
      {message: 'businessPartner.list.notification.copyId.success'},
      ObENotificationType.INFO
    );
  }));
});

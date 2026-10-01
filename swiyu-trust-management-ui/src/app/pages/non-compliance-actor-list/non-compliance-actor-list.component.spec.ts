import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {MatDialog} from '@angular/material/dialog';
import {provideRouter, Router, UrlTree} from '@angular/router';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {of, throwError} from 'rxjs';
import {NonCompliantActorApi} from '../../api/generated';
import {NonComplianceActorListComponent} from './non-compliance-actor-list.component';

describe('NonComplianceActorListComponent', () => {
  let component: NonComplianceActorListComponent;
  let fixture: ComponentFixture<NonComplianceActorListComponent>;
  let mockApi: jest.Mocked<NonCompliantActorApi>;
  let mockNotificationService: jest.Mocked<ObNotificationService>;
  let mockDialog: jest.Mocked<MatDialog>;

  const actor = {
    id: 'b2a2c5b6-0000-0000-0000-000000000001',
    did: 'did:test:abc',
    businessPartnerId: '1000000001',
    flaggedAsNonCompliantAt: '2026-01-01T00:00:00Z',
    reason: {reasonEn: 'Violation'}
  };

  beforeEach(async () => {
    mockApi = {
      getNonCompliantActors: jest.fn().mockReturnValue(of({content: [actor], page: {totalElements: 1}})),
      deleteNonCompliantActor: jest.fn().mockReturnValue(of(undefined))
    } as unknown as jest.Mocked<NonCompliantActorApi>;

    mockNotificationService = {
      send: jest.fn()
    } as unknown as jest.Mocked<ObNotificationService>;

    mockDialog = {
      open: jest.fn()
    } as unknown as jest.Mocked<MatDialog>;

    await TestBed.configureTestingModule({
      imports: [NonComplianceActorListComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        {provide: NonCompliantActorApi, useValue: mockApi},
        {provide: ObNotificationService, useValue: mockNotificationService}
      ]
    })
      .overrideProvider(MatDialog, {useValue: mockDialog})
      .compileComponents();

    TestBed.inject(TranslateService).use('en');

    fixture = TestBed.createComponent(NonComplianceActorListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges(false);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('links the add button to the create route', fakeAsync(() => {
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigateByUrl');
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-cy="addNonCompliantActor"]');
    btn.click();
    tick();
    expect(navigateSpy).toHaveBeenCalled();
    const urlTree = navigateSpy.mock.calls[0][0] as UrlTree;
    expect(urlTree.toString()).toBe('/non-compliance-actors/create');
  }));

  it('loads actors on init', () => {
    expect(mockApi.getNonCompliantActors).toHaveBeenCalled();
    expect(component.dataSource.data).toHaveLength(1);
  });

  it('sends the did and businessPartnerId filters to the API after debounce', fakeAsync(() => {
    component.filterForm.controls.did.setValue('did:test');
    component.filterForm.controls.businessPartnerId.setValue('1000');
    tick(300);
    const lastCall = mockApi.getNonCompliantActors.mock.calls.at(-1)![0] as Record<string, unknown>;
    expect(lastCall['did']).toBe('did:test');
    expect(lastCall['businessPartnerId']).toBe('1000');
  }));

  it('resets the page index when the filter changes', fakeAsync(() => {
    component.pageIndex = 3;
    component.filterForm.controls.did.setValue('did:test');
    tick(300);
    const lastCall = mockApi.getNonCompliantActors.mock.calls.at(-1)![0] as Record<string, unknown>;
    expect(lastCall['page']).toBe(0);
  }));

  it('shows a loading state while fetching', () => {
    expect(component.loading()).toBe(false);
  });

  it('shows the empty state when there are no results', fakeAsync(() => {
    mockApi.getNonCompliantActors.mockReturnValue(of({content: [], page: {totalElements: 0}}));
    component.filterForm.controls.did.setValue('nothing');
    tick(300);
    fixture.detectChanges();
    const noDataRow = fixture.nativeElement.querySelector('[data-cy="noDataRow"]');
    expect(noDataRow).not.toBeNull();
  }));

  it('reloads after a confirmed delete and steps back from the last page', () => {
    mockDialog.open.mockReturnValue({
      afterClosed: () => of(true)
    } as never);
    component.pageIndex = 2;
    component.dataSource.data = [actor];
    component.totalItems = 1;

    component.delete(actor);

    expect(mockApi.deleteNonCompliantActor).toHaveBeenCalledWith({
      nonCompliantActorId: actor.id
    });
    expect(component.pageIndex).toBe(1);
  });

  it('shows a success notification and reloads after a confirmed delete', () => {
    mockDialog.open.mockReturnValue({
      afterClosed: () => of(true)
    } as never);
    const callsBefore = mockApi.getNonCompliantActors.mock.calls.length;
    component.dataSource.data = [actor, actor];

    component.delete(actor);

    expect(mockNotificationService.send).toHaveBeenCalledWith(
      {message: 'nonCompliance.list.notification.delete.success'},
      ObENotificationType.INFO
    );
    expect(mockApi.getNonCompliantActors.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('does not reload and shows an error notification when delete fails', () => {
    mockDialog.open.mockReturnValue({
      afterClosed: () => of(true)
    } as never);
    mockApi.deleteNonCompliantActor.mockReturnValue(throwError(() => new Error('fail')));
    const callsBefore = mockApi.getNonCompliantActors.mock.calls.length;

    component.delete(actor);

    expect(mockNotificationService.send).toHaveBeenCalledWith(
      {message: 'nonCompliance.list.notification.delete.error'},
      ObENotificationType.ERROR
    );
    expect(mockApi.getNonCompliantActors.mock.calls.length).toBe(callsBefore);
  });

  it('does not delete when the confirmation is declined', () => {
    mockDialog.open.mockReturnValue({
      afterClosed: () => of(false)
    } as never);

    component.delete(actor);

    expect(mockApi.deleteNonCompliantActor).not.toHaveBeenCalled();
  });
});

import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {provideRouter, Router, UrlTree} from '@angular/router';
import {provideTranslateService} from '@ngx-translate/core';
import {ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {of, Subject, throwError} from 'rxjs';
import {NonCompliantActorApi} from '../../api/generated';
import {routes} from '../../app.routes';
import {NonComplianceActorCreateComponent} from './non-compliance-actor-create.component';

describe('NonComplianceActorCreateComponent', () => {
  let component: NonComplianceActorCreateComponent;
  let fixture: ComponentFixture<NonComplianceActorCreateComponent>;
  let mockApi: jest.Mocked<NonCompliantActorApi>;

  beforeEach(async () => {
    mockApi = {
      createNonCompliantActor: jest.fn().mockReturnValue(of({id: 'x', reason: {}}))
    } as unknown as jest.Mocked<NonCompliantActorApi>;

    await TestBed.configureTestingModule({
      imports: [NonComplianceActorCreateComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideRouter([]),
        provideTranslateService(),
        {provide: NonCompliantActorApi, useValue: mockApi},
        {provide: ObNotificationService, useValue: {send: jest.fn()}}
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NonComplianceActorCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the form on mount', () => {
    const form = fixture.nativeElement.querySelector('form');
    expect(form).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-cy="submitNonCompliantActor"]')).not.toBeNull();
  });

  it('disables submit until an identifier and a reason are provided and shows the reason hint', () => {
    fixture.detectChanges();
    const submit = fixture.nativeElement.querySelector('[data-cy="submitNonCompliantActor"]');
    expect(submit.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-cy="reasonHint"]')).not.toBeNull();
  });

  it('calls create with did and at least one reason', () => {
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();

    expect(mockApi.createNonCompliantActor).toHaveBeenCalledWith({
      nonCompliantActorRequest: {
        did: 'did:test:new',
        businessPartnerId: undefined,
        reason: {reasonDe: '', reasonFr: '', reasonIt: '', reasonEn: 'Violation', reasonRm: ''}
      }
    });
  });

  it('calls create with businessPartnerId and did undefined when businessPartnerId type is selected', () => {
    component.form.controls.identifierType.setValue('businessPartnerId');
    component.form.controls.businessPartnerId.setValue('10000000-0000-0000-0000-000000000001');
    component.form.controls.reasonDe.setValue('Grund');

    component.submit();

    expect(mockApi.createNonCompliantActor).toHaveBeenCalledWith({
      nonCompliantActorRequest: {
        did: undefined,
        businessPartnerId: '10000000-0000-0000-0000-000000000001',
        reason: {reasonDe: 'Grund', reasonFr: '', reasonIt: '', reasonEn: '', reasonRm: ''}
      }
    });
  });

  it('does not call create when neither did nor businessPartnerId is set', () => {
    component.form.controls.identifierType.setValue('did');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();

    expect(mockApi.createNonCompliantActor).not.toHaveBeenCalled();
  });

  it('rejects a businessPartnerId that is not a valid UUID', () => {
    component.form.controls.identifierType.setValue('businessPartnerId');
    component.form.controls.businessPartnerId.setValue('not-a-uuid');
    component.form.controls.reasonDe.setValue('Grund');

    component.submit();

    expect(mockApi.createNonCompliantActor).not.toHaveBeenCalled();
  });

  it('renders an inline error on the businessPartnerId field for an invalid UUID', () => {
    component.form.controls.identifierType.setValue('businessPartnerId');
    component.form.controls.businessPartnerId.setValue('not-a-uuid');
    fixture.detectChanges();

    const error = fixture.nativeElement.querySelector('[data-cy="businessPartnerIdError"]');
    expect(error).toBeTruthy();
  });

  it('accepts a businessPartnerId that is a valid UUID', () => {
    component.form.controls.identifierType.setValue('businessPartnerId');
    component.form.controls.businessPartnerId.setValue('3f2b5c6e-1a2b-4c3d-8e9f-0a1b2c3d4e5f');
    component.form.controls.reasonDe.setValue('Grund');

    component.submit();

    expect(mockApi.createNonCompliantActor).toHaveBeenCalled();
  });

  it('does not flag businessPartnerId or block submit when identifierType is did, even with a malformed leftover value', () => {
    component.form.controls.identifierType.setValue('did');
    component.form.controls.businessPartnerId.setValue('not-a-uuid');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');
    fixture.detectChanges();

    expect(component.form.valid).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-cy="businessPartnerIdError"]')).toBeFalsy();
    component.submit();
    expect(mockApi.createNonCompliantActor).toHaveBeenCalled();
  });

  it('navigates to the list after a successful create', () => {
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();

    expect(navigate).toHaveBeenCalledWith(['/non-compliance-actors', 'x']);
  });

  it('registers the create route before the :actorId route', () => {
    const idxCreate = routes.findIndex(r => r.path === 'non-compliance-actors/create');
    const idxDetail = routes.findIndex(r => r.path === 'non-compliance-actors/:actorId');
    expect(idxCreate).toBeGreaterThanOrEqual(0);
    expect(idxCreate).toBeLessThan(idxDetail);
  });

  it('form is invalid when no reason is set', () => {
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    expect(component.form.valid).toBe(false);
  });

  it('form is valid for a did with a reason', () => {
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');
    expect(component.form.valid).toBe(true);
  });

  it('form is invalid for an invalid businessPartnerId UUID', () => {
    component.form.controls.identifierType.setValue('businessPartnerId');
    component.form.controls.businessPartnerId.setValue('not-a-uuid');
    component.form.controls.reasonDe.setValue('Grund');
    expect(component.form.valid).toBe(false);
  });

  it('does not submit a second time while a request is in flight', () => {
    const pending = new Subject();
    mockApi.createNonCompliantActor.mockReturnValue(pending as never);
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();
    component.submit();
    pending.complete();

    expect(mockApi.createNonCompliantActor).toHaveBeenCalledTimes(1);
  });

  it('sets loading true while the request is in flight and false afterwards', () => {
    const pending = new Subject();
    mockApi.createNonCompliantActor.mockReturnValue(pending as never);
    jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();
    expect(component.loading()).toBe(true);
    pending.next({id: 'x', reason: {}});
    pending.complete();
    expect(component.loading()).toBe(false);
  });

  it('shows an error notification and does not navigate when create fails', () => {
    mockApi.createNonCompliantActor.mockReturnValue(throwError(() => new Error('boom')));
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigate');
    const notificationService = TestBed.inject(ObNotificationService);
    component.form.controls.identifierType.setValue('did');
    component.form.controls.did.setValue('did:test:new');
    component.form.controls.reasonEn.setValue('Violation');

    component.submit();

    expect(notificationService.send).toHaveBeenCalledWith(
      {message: 'nonCompliance.create.notification.error'},
      ObENotificationType.ERROR
    );
    expect(navigate).not.toHaveBeenCalled();
  });

  it('links the cancel button to the list route', fakeAsync(() => {
    const router = TestBed.inject(Router);
    const navigateSpy = jest.spyOn(router, 'navigateByUrl');
    fixture.detectChanges();
    const btn = fixture.nativeElement.querySelector('[data-cy="cancelNonCompliantActor"]');
    btn.click();
    tick();
    expect(navigateSpy).toHaveBeenCalled();
    const urlTree = navigateSpy.mock.calls[0][0] as UrlTree;
    expect(urlTree.toString()).toBe('/non-compliance-actors');
    expect(mockApi.createNonCompliantActor).not.toHaveBeenCalled();
  }));
});

import {provideHttpClient} from '@angular/common/http';
import {provideZoneChangeDetection} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgForm} from '@angular/forms';
import {provideTranslateService, TranslateService} from '@ngx-translate/core';
import {ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {of, throwError} from 'rxjs';
import {TaskApi} from '../../../api/generated';
import {AppConfigService} from '../../../core/appconfig/app-config.service';
import {BusinessPartnerDeletePanelComponent} from './business-partner-delete-panel.component';
import {BusinessPartnerDeletePanelService} from './business-partner-delete-panel.service';

describe('BusinessPartnerDeletePanelComponent', () => {
  let fixture: ComponentFixture<BusinessPartnerDeletePanelComponent>;
  let component: BusinessPartnerDeletePanelComponent;
  let panelService: BusinessPartnerDeletePanelService;
  let api: jest.Mocked<Pick<TaskApi, 'createDeleteBusinessPartnerTask'>>;

  const PARTNER = {businessPartnerId: 'bp-1', name: 'Acme AG'};
  const validForm = {invalid: false} as NgForm;

  beforeEach(async () => {
    api = {createDeleteBusinessPartnerTask: jest.fn().mockReturnValue(of(undefined))};

    await TestBed.configureTestingModule({
      imports: [BusinessPartnerDeletePanelComponent],
      providers: [
        provideZoneChangeDetection({eventCoalescing: true}),
        provideHttpClient(),
        provideTranslateService(),
        {provide: TaskApi, useValue: api},
        {provide: AppConfigService, useValue: {hardDeleteDelaySeconds: 172800}},
        {provide: ObNotificationService, useValue: {send: jest.fn()}}
      ]
    }).compileComponents();

    TestBed.inject(TranslateService).use('en');
    fixture = TestBed.createComponent(BusinessPartnerDeletePanelComponent);
    component = fixture.componentInstance;
    panelService = TestBed.inject(BusinessPartnerDeletePanelService);
    fixture.detectChanges();
  });

  it('submits the request with the reason once the partner is selected and the confirmation is ticked', () => {
    panelService.open(PARTNER);
    component.reason.set('  no longer in business  ');
    component.confirmed.set(true);

    component.submit(validForm);

    expect(api.createDeleteBusinessPartnerTask).toHaveBeenCalledWith({
      createDeleteBusinessPartnerTaskRequest: {
        businessPartnerId: 'bp-1',
        reason: 'no longer in business',
        irreversibleDeletionConfirmed: true
      }
    });
  });

  it('does not submit without the confirmation and shows the error instead', () => {
    panelService.open(PARTNER);
    component.reason.set('a reason');
    component.confirmed.set(false);

    component.submit(validForm);

    expect(api.createDeleteBusinessPartnerTask).not.toHaveBeenCalled();
    expect(component.confirmationError()).toBe(true);
  });

  it('does not submit an empty reason', () => {
    panelService.open(PARTNER);
    component.confirmed.set(true);
    component.reason.set('   ');

    component.submit(validForm);

    expect(api.createDeleteBusinessPartnerTask).not.toHaveBeenCalled();
  });

  it('clears the form when another partner is opened', () => {
    panelService.open(PARTNER);
    component.reason.set('something');
    component.confirmed.set(true);

    panelService.open({businessPartnerId: 'bp-2', name: 'Other AG'});

    expect(component.reason()).toBe('');
    expect(component.confirmed()).toBe(false);
  });

  it('stops submitting when the backend rejects the partner', () => {
    api.createDeleteBusinessPartnerTask.mockReturnValue(throwError(() => new Error('not deletable')));
    panelService.open(PARTNER);
    component.reason.set('a reason');
    component.confirmed.set(true);

    component.submit(validForm);

    expect(component.submitting()).toBe(false);
  });
  it('displays the configured waiting period in the confirmation', () => {
    TestBed.inject(TranslateService).setTranslation('en', {
      'businessPartner.delete.confirm': 'Delete after {{delay}}'
    });
    panelService.open(PARTNER);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Delete after 2 days');
    (TestBed.inject(AppConfigService) as {hardDeleteDelaySeconds: number}).hardDeleteDelaySeconds = 300;
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Delete after 5 minutes');
  });

  it('does not submit until the waiting period is known', () => {
    (TestBed.inject(AppConfigService) as {hardDeleteDelaySeconds: number | undefined}).hardDeleteDelaySeconds =
      undefined;
    panelService.open(PARTNER);
    component.reason.set('reason');
    component.confirmed.set(true);
    component.submit(validForm);
    expect(api.createDeleteBusinessPartnerTask).not.toHaveBeenCalled();
  });

  it('translates the eligibility code returned by the backend and keeps the form open', () => {
    api.createDeleteBusinessPartnerTask.mockReturnValue(
      throwError(() => ({status: 400, error: 'PARTNER_NOT_CLEARED_FOR_DELETION'}))
    );
    panelService.open(PARTNER);
    component.reason.set('reason');
    component.confirmed.set(true);
    component.submit(validForm);
    expect(TestBed.inject(ObNotificationService).send).toHaveBeenCalledWith({
      message: 'businessPartner.delete.notification.error.notCleared',
      type: ObENotificationType.ERROR
    });
    expect(component.partner()).toEqual(PARTNER);
    expect(component.reason()).toBe('reason');
  });

  it('falls back to the generic message for an unknown error', () => {
    api.createDeleteBusinessPartnerTask.mockReturnValue(throwError(() => ({status: 500, error: 'boom'})));
    panelService.open(PARTNER);
    component.reason.set('reason');
    component.confirmed.set(true);
    component.submit(validForm);
    expect(TestBed.inject(ObNotificationService).send).toHaveBeenCalledWith({
      message: 'businessPartner.delete.notification.error',
      type: ObENotificationType.ERROR
    });
  });
});

import {Component, inject, signal} from '@angular/core';
import {MatCheckbox} from '@angular/material/checkbox';
import {TranslatePipe, TranslateService} from '@ngx-translate/core';
import {PanelData, SidepanelService} from './sidepanel.service';

import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule, NgForm} from '@angular/forms';
import {MatButton} from '@angular/material/button';
import {MatFormField, MatHint, MatLabel} from '@angular/material/form-field';
import {MatInput} from '@angular/material/input';
import {MatOption, MatSelect} from '@angular/material/select';
import {ObButtonDirective} from '@oblique/oblique';
import {
  TrustOnboardingDeclineReason,
  TrustOnboardingRejectReason,
  TrustOnboardingTaskAction,
  TrustOnboardingTaskApi
} from '../../../api/generated';

interface FormData {
  partnerMessageBody: string;
  rejectReason?: TrustOnboardingRejectReason;
  declineReason?: TrustOnboardingDeclineReason;
  internalMessageBody: string;
  declarationAccepted: boolean;
}

@Component({
  selector: 'app-sidepanel',
  imports: [
    TranslatePipe,
    MatCheckbox,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    MatSelect,
    MatOption,
    FormsModule,
    ObButtonDirective,
    MatButton
  ],
  templateUrl: './sidepanel.component.html',
  styleUrl: './sidepanel.component.scss'
})
export class SidepanelComponent {
  private readonly sidepanelService = inject(SidepanelService);
  private readonly api = inject(TrustOnboardingTaskApi);
  private readonly translateService = inject(TranslateService);

  TrustOnboardingTaskAction = TrustOnboardingTaskAction;

  rejectReasons = Object.values(TrustOnboardingRejectReason);
  declineReasons = Object.values(TrustOnboardingDeclineReason);

  formData = signal(this.initialForm());
  panelData = signal<PanelData | null>(null);
  declarationCheckVisible = signal(false);
  declarationError = signal(false);
  submitLabelKey = signal<string>('');
  correspondenceLanguageKey = signal<string>('');

  constructor() {
    this.sidepanelService.panelData$.pipe(takeUntilDestroyed()).subscribe(data => {
      this.formData.set(this.initialForm());
      this.panelData.set(data);
      this.submitLabelKey.set(getSubmitLabelKey(this.panelData()?.action));
      this.correspondenceLanguageKey.set(
        this.getCorrespondenceLanguageKey(this.panelData()?.task?.correspondenceLanguage)
      );
      this.declarationError.set(false);
      if (this.panelData()) {
        this.declarationCheckVisible.set(this.panelData()?.action !== TrustOnboardingTaskAction.AddInternalNote);
      }
    });
    this.translateSetup();
  }

  submit(form: NgForm) {
    if (form.invalid) {
      console.warn('Form is invalid');
      return;
    }

    const errors: string[] = [];

    // Rule 1: Declaration must be accepted (except when adding internal notes)
    if (this.declarationCheckVisible() && !this.formData().declarationAccepted) {
      this.declarationError.set(true);
      errors.push('Declaration must be accepted.');
    }

    // Rule 2: Reject-specific validation
    if (this.panelData()?.action === TrustOnboardingTaskAction.Reject) {
      const reason = this.formData().rejectReason;
      if (!reason) {
        errors.push('Reject reason is required.');
      } else if (!this.rejectReasons.includes(reason)) {
        errors.push('Reject reason must be one of the predefined values.');
      }
    }

    if (errors.length > 0) {
      console.warn('Validation failed:', errors);
      // Optionally show errors in the UI
      return;
    }

    switch (this.panelData()?.action) {
      case TrustOnboardingTaskAction.Reject:
        this.api
          .reject({
            taskId: this.panelData()?.task?.id as string,
            request: {
              internalNote: this.formData().internalMessageBody,
              partnerNote: this.formData().partnerMessageBody,
              rejectReason: this.formData().rejectReason! // can never be null when submitting
            }
          })
          .subscribe(() => {
            this.sidepanelService.closePanel();
          });
        break;
      case TrustOnboardingTaskAction.RequestMoreInformation:
        this.api
          .requestMoreInformation({
            taskId: this.panelData()?.task?.id as string,
            request: {
              internalNote: this.formData().internalMessageBody,
              partnerNote: this.formData().partnerMessageBody,
              declineReason: this.formData().declineReason! // can never be null when submitting
            }
          })
          .subscribe(() => {
            this.sidepanelService.closePanel();
          });
        break;
      case TrustOnboardingTaskAction.Approve:
        this.api
          .approve({
            taskId: this.panelData()?.task?.id as string,
            request: {
              internalNote: this.formData().internalMessageBody,
              partnerNote: this.formData().partnerMessageBody
            }
          })
          .subscribe(() => {
            this.sidepanelService.closePanel();
          });
        break;
      case TrustOnboardingTaskAction.AddInternalNote:
        this.api
          .addInternalNote({
            taskId: this.panelData()?.task?.id as string,
            request: {
              internalNote: this.formData().internalMessageBody
            }
          })
          .subscribe(() => {
            this.sidepanelService.closePanel();
          });
        break;
    }
  }

  private getCorrespondenceLanguageKey(correspondanceLanguage?: string) {
    return 'app.trust-onboarding-task.sidepanel.message.hint.' + correspondanceLanguage;
  }

  private translateSetup() {
    // Required for translate service auto collection of i18n keys
    this.translateService.get('app.trust-onboarding-task.sidepanel.submit.request-information');
    this.translateService.get('app.trust-onboarding-task.sidepanel.submit.note');
    this.translateService.get('app.trust-onboarding-task.sidepanel.submit.approve-request');
    this.translateService.get('app.trust-onboarding-task.sidepanel.submit.reject-request');

    this.translateService.get('app.trust-onboarding-task.sidepanel.submit.reject-request');

    this.translateService.get('app.trust-onboarding-task.sidepanel.message.hint.de-CH');
    this.translateService.get('app.trust-onboarding-task.sidepanel.message.hint.en');
    this.translateService.get('app.trust-onboarding-task.sidepanel.message.hint.fr-CH');
    this.translateService.get('app.trust-onboarding-task.sidepanel.message.hint.it-CH');
    this.translateService.get('app.trust-onboarding-task.sidepanel.message.hint.rm-CH');

    this.translateService.get('app.trust-onboarding-task.sidepanel.decline-reason.type.MISSING_DOCUMENTS');
    this.translateService.get('app.trust-onboarding-task.sidepanel.decline-reason.type.UNAUTHORIZED_SIGNATORIES');
    this.translateService.get('app.trust-onboarding-task.sidepanel.decline-reason.type.INCORRECT_COMPANY_INFORMATION');
    this.translateService.get(
      'app.trust-onboarding-task.sidepanel.decline-reason.type.INCORRECT_DECLARATION_OF_INTENT'
    );
    this.translateService.get('app.trust-onboarding-task.sidepanel.decline-reason.type.OTHER');

    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.INCOMPLETE_INFORMATION');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.INACCURATE_INFORMATION');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.OUTDATED_INFORMATION');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.IDENTITY_VERIFICATION_FAILURE');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.LACK_OF_AUTHORIZATION');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.TECHNICAL_ISSUES');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.DUPLICATE_APPLICATION');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.NO_RESPONSE_FROM_APPLICANT');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.FRAUDULENT_ACTIVITY');
    this.translateService.get('app.trust-onboarding-task.sidepanel.reject-reason.type.OTHER');
  }

  private initialForm(): FormData {
    return {
      partnerMessageBody: '',
      rejectReason: undefined,
      declineReason: undefined,
      internalMessageBody: '',
      declarationAccepted: false
    };
  }
}

function getSubmitLabelKey(action?: TrustOnboardingTaskAction): string {
  switch (action) {
    case TrustOnboardingTaskAction.RequestMoreInformation:
      return 'app.trust-onboarding-task.sidepanel.submit.request-information';
    case TrustOnboardingTaskAction.AddInternalNote:
      return 'app.trust-onboarding-task.sidepanel.submit.note';
    case TrustOnboardingTaskAction.Approve:
      return 'app.trust-onboarding-task.sidepanel.submit.approve-request';
    case TrustOnboardingTaskAction.Reject:
      return 'app.trust-onboarding-task.sidepanel.submit.reject-request';
    default:
      return 'Submit';
  }
}

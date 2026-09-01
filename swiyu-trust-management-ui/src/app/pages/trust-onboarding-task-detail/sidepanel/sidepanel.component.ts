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
import {ObAlertComponent, ObButtonDirective} from '@oblique/oblique';
import {Observable} from 'rxjs';
import {TaskAction, TaskApi, TrustOnboardingRejectReason} from '../../../api/generated';

interface FormData {
  partnerMessageBody: string;
  rejectReason?: TrustOnboardingRejectReason;
  protectedVerificationRejectReason: string;
  internalMessageBody: string;
  declarationAccepted: boolean;
}

const PROTECTED_VERIFICATION_REQUEST_TASK_TYPE = 'PROTECTED_VERIFICATION_REQUEST';

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
    MatButton,
    ObAlertComponent
  ],
  templateUrl: './sidepanel.component.html',
  styleUrl: './sidepanel.component.scss'
})
export class SidepanelComponent {
  private readonly sidepanelService = inject(SidepanelService);
  private readonly api = inject(TaskApi);
  private readonly translateService = inject(TranslateService);

  TaskAction = TaskAction;

  rejectReasons = Object.values(TrustOnboardingRejectReason);

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
      const task = this.panelData()?.task;
      this.correspondenceLanguageKey.set(
        this.getCorrespondenceLanguageKey(
          task && 'correspondenceLanguage' in task ? task.correspondenceLanguage : undefined
        )
      );
      this.declarationError.set(false);
      if (this.panelData()) {
        this.declarationCheckVisible.set(
          this.panelData()?.action !== TaskAction.AddInternalNote && !this.isProtectedVerificationTask()
        );
      }
    });
    this.translateSetup();
  }

  isProtectedVerificationTask(): boolean {
    const task = this.panelData()?.task;
    return !!task && 'taskType' in task && task.taskType === PROTECTED_VERIFICATION_REQUEST_TASK_TYPE;
  }

  submit(form: NgForm) {
    if (form.invalid) {
      console.warn('Form is invalid');
      return;
    }

    if (!this.validate()) {
      return;
    }

    this.buildRequest()?.subscribe(() => {
      this.sidepanelService.closePanel();
    });
  }

  private validate(): boolean {
    const errors: string[] = [];

    // Rule 1: Declaration must be accepted (except when adding internal notes, or for protected verification tasks)
    if (this.declarationCheckVisible() && !this.formData().declarationAccepted) {
      this.declarationError.set(true);
      errors.push('Declaration must be accepted.');
    }

    // Rule 2: Reject-specific validation
    if (this.panelData()?.action === TaskAction.Reject) {
      if (this.isProtectedVerificationTask()) {
        if (!this.formData().protectedVerificationRejectReason.trim()) {
          errors.push('Reject reason is required.');
        }
      } else {
        const reason = this.formData().rejectReason;
        if (!reason) {
          errors.push('Reject reason is required.');
        } else if (!this.rejectReasons.includes(reason)) {
          errors.push('Reject reason must be one of the predefined values.');
        }
      }
    }

    if (errors.length > 0) {
      console.warn('Validation failed:', errors);
      // Optionally show errors in the UI
      return false;
    }
    return true;
  }

  private buildRequest(): Observable<unknown> | undefined {
    const taskId = this.panelData()?.task?.id as string;
    const isProtectedVerificationTask = this.isProtectedVerificationTask();

    switch (this.panelData()?.action) {
      case TaskAction.Reject:
        return isProtectedVerificationTask
          ? this.api.rejectProtectedVerificationRequest({
              taskId,
              request: {
                internalNote: this.formData().internalMessageBody,
                rejectReason: this.formData().protectedVerificationRejectReason
              }
            })
          : this.api.rejectTrustOnboarding({
              taskId,
              request: {
                internalNote: this.formData().internalMessageBody,
                partnerNote: this.formData().partnerMessageBody,
                rejectReason: this.formData().rejectReason! // can never be null when submitting
              }
            });
      case TaskAction.RequestMoreInformation:
        return this.api.requestMoreInformationForTrustOnboarding({
          taskId,
          request: {
            internalNote: this.formData().internalMessageBody,
            partnerNote: this.formData().partnerMessageBody
          }
        });
      case TaskAction.Approve:
        return isProtectedVerificationTask
          ? this.api.approveProtectedVerificationRequest({
              taskId,
              request: {internalNote: this.formData().internalMessageBody}
            })
          : this.api.approveTrustOnboarding({
              taskId,
              request: {
                internalNote: this.formData().internalMessageBody,
                partnerNote: this.formData().partnerMessageBody
              }
            });
      case TaskAction.AddInternalNote:
        return this.api.addInternalNote({
          taskId,
          request: {internalNote: this.formData().internalMessageBody}
        });
      default:
        return undefined;
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
      protectedVerificationRejectReason: '',
      internalMessageBody: '',
      declarationAccepted: false
    };
  }
}

function getSubmitLabelKey(action?: TaskAction): string {
  switch (action) {
    case TaskAction.RequestMoreInformation:
      return 'app.trust-onboarding-task.sidepanel.submit.request-information';
    case TaskAction.AddInternalNote:
      return 'app.trust-onboarding-task.sidepanel.submit.note';
    case TaskAction.Approve:
      return 'app.trust-onboarding-task.sidepanel.submit.approve-request';
    case TaskAction.Reject:
      return 'app.trust-onboarding-task.sidepanel.submit.reject-request';
    default:
      return 'Submit';
  }
}

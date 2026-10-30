import {Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormsModule, NgForm} from '@angular/forms';
import {MatButton} from '@angular/material/button';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatFormField, MatLabel} from '@angular/material/form-field';
import {MatInput} from '@angular/material/input';
import {_, TranslatePipe, TranslateService} from '@ngx-translate/core';
import {ObButtonDirective, ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {catchError, EMPTY, finalize} from 'rxjs';
import {BusinessPartnerViewListItem, TaskApi} from '../../../api/generated';
import {AppConfigService} from '../../../core/appconfig/app-config.service';
import {BusinessPartnerDeletePanelService} from './business-partner-delete-panel.service';

/**
 * Translations for the eligibility codes of the backend's BusinessPartnerDeletionError. The backend only sends the
 * code, because its own message is English and names the partner by uuid.
 */
const ELIGIBILITY_ERROR_KEYS: Record<string, string> = {
  PARTNER_NOT_SYNCED: _('businessPartner.delete.notification.error.notSynced'),
  PARTNER_NOT_CLEARED_FOR_DELETION: _('businessPartner.delete.notification.error.notCleared'),
  OPEN_DELETION_REQUEST_EXISTS: _('businessPartner.delete.notification.error.alreadyRequested')
};

/** Falls back to the generic message for anything that is not a known eligibility code. */
function eligibilityErrorKey(error: {status?: number; error?: unknown}): string {
  const code = error.status === 400 && typeof error.error === 'string' ? error.error.trim() : '';
  return ELIGIBILITY_ERROR_KEYS[code] ?? _('businessPartner.delete.notification.error');
}

@Component({
  selector: 'app-business-partner-delete-panel',
  imports: [TranslatePipe, FormsModule, MatCheckbox, MatFormField, MatLabel, MatInput, MatButton, ObButtonDirective],
  templateUrl: './business-partner-delete-panel.component.html',
  styleUrl: './business-partner-delete-panel.component.scss'
})
export class BusinessPartnerDeletePanelComponent {
  private readonly panelService = inject(BusinessPartnerDeletePanelService);
  private readonly api = inject(TaskApi);
  private readonly appConfigService = inject(AppConfigService);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(ObNotificationService);

  readonly partner = signal<BusinessPartnerViewListItem | null>(null);
  readonly reason = signal('');
  readonly confirmed = signal(false);
  readonly confirmationError = signal(false);
  readonly submitting = signal(false);

  constructor() {
    this.panelService.partner$.pipe(takeUntilDestroyed()).subscribe(partner => {
      this.partner.set(partner);
      this.reason.set('');
      this.confirmed.set(false);
      this.confirmationError.set(false);
    });
  }

  get waitingPeriod(): string | undefined {
    const seconds = this.appConfigService.hardDeleteDelaySeconds;
    if (seconds === undefined) {
      return undefined;
    }
    const units = [
      {unit: 'day', divisor: 86400},
      {unit: 'hour', divisor: 3600},
      {unit: 'minute', divisor: 60}
    ];
    const {unit, divisor} = units.find(candidate => seconds > 0 && seconds % candidate.divisor === 0) ?? {
      unit: 'second',
      divisor: 1
    };
    return new Intl.NumberFormat(this.translateService.getCurrentLang() || 'de', {
      style: 'unit',
      unit,
      unitDisplay: 'long'
    }).format(seconds / divisor);
  }

  cancel(): void {
    this.panelService.close();
  }

  submit(form: NgForm): void {
    // The checkbox is not a form control the browser can mark invalid, so it is validated explicitly.
    if (!this.confirmed()) {
      this.confirmationError.set(true);
      return;
    }
    if (form.invalid || !this.reason().trim() || this.submitting() || this.waitingPeriod === undefined) {
      return;
    }

    const businessPartnerId = this.partner()?.businessPartnerId;
    if (!businessPartnerId) {
      return;
    }

    this.submitting.set(true);
    this.api
      .createDeleteBusinessPartnerTask({
        createDeleteBusinessPartnerTaskRequest: {
          businessPartnerId,
          reason: this.reason().trim(),
          irreversibleDeletionConfirmed: true
        }
      })
      .pipe(
        catchError(error => {
          this.notify(eligibilityErrorKey(error), ObENotificationType.ERROR);
          return EMPTY;
        }),
        finalize(() => this.submitting.set(false))
      )
      .subscribe(() => {
        this.notify(_('businessPartner.delete.notification.success'), ObENotificationType.INFO);
        this.panelService.closeAfterCreate();
      });
  }

  private notify(messageKey: string, type: ObENotificationType): void {
    this.translateService.get(messageKey).subscribe(message => {
      this.notificationService.send({message, type});
    });
  }
}

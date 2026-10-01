import {Component, inject, signal} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors
} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatRadioModule} from '@angular/material/radio';
import {Router, RouterLink} from '@angular/router';
import {_, TranslateModule, TranslateService} from '@ngx-translate/core';
import {ObButtonDirective, ObDocumentMetaService, ObENotificationType, ObNotificationService} from '@oblique/oblique';
import {finalize, take} from 'rxjs';
import {NonCompliantActorApi} from '../../api/generated';

type IdentifierType = 'did' | 'businessPartnerId';

interface CreateFormModel {
  identifierType: FormControl<IdentifierType>;
  did: FormControl<string>;
  businessPartnerId: FormControl<string>;
  reasonDe: FormControl<string>;
  reasonFr: FormControl<string>;
  reasonIt: FormControl<string>;
  reasonEn: FormControl<string>;
  reasonRm: FormControl<string>;
}

const REASON_LANGUAGES = ['de', 'fr', 'it', 'en', 'rm'];

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export const UUID_PATTERN = UUID_REGEX.source;

function reasonControlName(lang: string): keyof CreateFormModel {
  return ('reason' + lang.charAt(0).toUpperCase() + lang.slice(1)) as keyof CreateFormModel;
}

function uuidValidator(control: AbstractControl): ValidationErrors | null {
  const parent = control.parent as FormGroup<CreateFormModel> | null;
  if (parent?.controls.identifierType.value !== 'businessPartnerId') {
    return null;
  }
  const value = (control.value as string | undefined)?.trim();
  return value && !UUID_REGEX.test(value) ? {invalidUuid: true} : null;
}

function nonComplianceActorFormValidator(group: AbstractControl): ValidationErrors | null {
  const form = group as FormGroup<CreateFormModel>;
  const type = form.controls.identifierType.value;
  const identifier = (type === 'did' ? form.controls.did.value : form.controls.businessPartnerId.value)?.trim();
  const hasReason = REASON_LANGUAGES.some(lang => form.controls[reasonControlName(lang)].value?.trim());
  return !identifier || !hasReason ? {invalid: true} : null;
}

@Component({
  selector: 'app-non-compliance-actor-create',
  standalone: true,
  templateUrl: './non-compliance-actor-create.component.html',
  styleUrls: ['./non-compliance-actor-create.component.scss'],
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    TranslateModule,
    ObButtonDirective,
    RouterLink
  ]
})
export class NonComplianceActorCreateComponent {
  readonly reasonLanguages = REASON_LANGUAGES;
  readonly form: FormGroup<CreateFormModel>;
  readonly loading = signal(false);
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(NonCompliantActorApi);
  private readonly router = inject(Router);
  private readonly meta = inject(ObDocumentMetaService);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(ObNotificationService);

  constructor() {
    this.meta.setTitle('nonCompliance.create.title');
    this.form = this.fb.group(
      {
        identifierType: this.fb.nonNullable.control<IdentifierType>('did'),
        did: this.fb.nonNullable.control(''),
        businessPartnerId: this.fb.nonNullable.control('', {validators: [uuidValidator]}),
        reasonDe: this.fb.nonNullable.control(''),
        reasonFr: this.fb.nonNullable.control(''),
        reasonIt: this.fb.nonNullable.control(''),
        reasonEn: this.fb.nonNullable.control(''),
        reasonRm: this.fb.nonNullable.control('')
      },
      {validators: [nonComplianceActorFormValidator]}
    );
    this.form.controls.identifierType.valueChanges.subscribe(() => {
      this.form.controls.did.updateValueAndValidity();
      this.form.controls.businessPartnerId.updateValueAndValidity();
    });
  }

  reasonKey(lang: string): keyof CreateFormModel {
    return reasonControlName(lang);
  }

  submit(): void {
    if (!this.form.valid || this.loading()) {
      return;
    }
    const f = this.form.getRawValue();
    this.loading.set(true);
    this.api
      .createNonCompliantActor({
        nonCompliantActorRequest: {
          did: f.identifierType === 'did' ? f.did?.trim() : undefined,
          businessPartnerId: f.identifierType === 'businessPartnerId' ? f.businessPartnerId?.trim() : undefined,
          reason: {
            reasonDe: f.reasonDe,
            reasonFr: f.reasonFr,
            reasonIt: f.reasonIt,
            reasonEn: f.reasonEn,
            reasonRm: f.reasonRm
          }
        }
      })
      .pipe(
        take(1),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: created => this.router.navigate(['/non-compliance-actors', created.id]),
        error: () => this.notify(_('nonCompliance.create.notification.error'), ObENotificationType.ERROR)
      });
  }

  private notify(messageKey: string, type: ObENotificationType): void {
    this.translateService
      .get(messageKey)
      .pipe(take(1))
      .subscribe(message => this.notificationService.send({message}, type));
  }
}

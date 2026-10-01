import {Pipe, PipeTransform, inject} from '@angular/core';
import {NonCompliantReasonText} from '../../api/generated';
import {LocalizeService} from '../../core/i18n/localize.service';

const REASON_PRIORITY: string[] = ['de', 'en', 'fr', 'it', 'rm'];

@Pipe({
  name: 'nonComplianceReason',
  pure: false // Impure so the value updates when the language changes.
})
export class NonComplianceReasonPipe implements PipeTransform {
  private readonly localizeService = inject(LocalizeService);

  transform(reason: NonCompliantReasonText | null | undefined): string {
    if (!reason) {
      return '';
    }
    const byLang: Record<string, string | undefined> = {
      de: reason.reasonDe,
      en: reason.reasonEn,
      fr: reason.reasonFr,
      it: reason.reasonIt,
      rm: reason.reasonRm
    };
    const currentLang = (this.localizeService.currentLang() ?? '').split('-')[0];
    const order = currentLang
      ? [currentLang, ...REASON_PRIORITY.filter(language => language !== currentLang)]
      : REASON_PRIORITY;
    for (const language of order) {
      const value = byLang[language];
      if (value && value.trim()) {
        return value;
      }
    }
    return '';
  }
}

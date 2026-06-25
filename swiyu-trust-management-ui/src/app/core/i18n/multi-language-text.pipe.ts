import {Pipe, PipeTransform, inject} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {MultiLanguageText} from '../../api/generated';

@Pipe({name: 'multiLanguageText'})
export class MultiLanguageTextPipe implements PipeTransform {
  private translate = inject(TranslateService);

  transform(value: MultiLanguageText | null | undefined): string {
    if (!value) return '';
    const lang = (this.translate.currentLang || this.translate.getDefaultLang() || 'de').toLowerCase();
    return value[lang as keyof MultiLanguageText] || '';
  }
}

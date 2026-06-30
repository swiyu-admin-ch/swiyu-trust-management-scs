import {computed, inject, Injectable, Signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {TranslateService} from '@ngx-translate/core';
import {map} from 'rxjs/operators';
import {DEFAULT_LOCALIZED_TEXT_KEY, LocalizedTextMap} from './localized-map.util';

@Injectable({providedIn: 'root'})
export class LocalizeService {
  private readonly translate = inject(TranslateService);

  readonly currentLang = toSignal(this.translate.onLangChange.pipe(map(event => event.lang)), {
    initialValue: this.translate.currentLang
  });

  localizeSignal(mapSource: () => LocalizedTextMap | null | undefined): Signal<string> {
    return computed(() => this.localize(mapSource()));
  }

  localize(map: LocalizedTextMap | null | undefined): string {
    if (!map) {
      return '';
    }
    return this.getLocalizedText(map, this.currentLang());
  }
  private getLocalizedText(value: LocalizedTextMap, language: string): string {
    // try exact match
    if (value[language]) {
      return value[language];
    }

    // try base language if available
    const baseLang = language.split('-')[0];
    if (value[baseLang]) {
      return value[baseLang];
    }

    // try regional variant
    const regionalKey = Object.keys(value).find(key => key.startsWith(`${language}-`));
    if (regionalKey) {
      return value[regionalKey];
    }

    // default fallback
    return value[DEFAULT_LOCALIZED_TEXT_KEY] || '';
  }
}

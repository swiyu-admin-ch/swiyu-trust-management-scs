import {Pipe, PipeTransform, inject} from '@angular/core';
import {LocalizeService} from './localize.service';
import {LocalizedTextMap} from './localized-map.util';

@Pipe({
  name: 'localize',
  pure: false // Impure so the value updates when the language changes without needing signals for all localized values.
})
export class LocalizePipe implements PipeTransform {
  private readonly localizeService = inject(LocalizeService);

  transform(value: LocalizedTextMap | null | undefined): string {
    return this.localizeService.localize(value);
  }
}

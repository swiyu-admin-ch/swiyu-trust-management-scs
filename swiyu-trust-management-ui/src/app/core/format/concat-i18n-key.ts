import {Pipe, PipeTransform} from '@angular/core';

/**
 * Concatenates an i18n translation key with a value.
 *
 * Useful for building dynamic translation keys in templates
 * without manual string concatenation or null checks.
 *
 * @example
 * <!-- value is defined -->
 * {{ 'active' | concatI18nKey:'app.status' | translate }}
 * <!-- results in: translate('app.status.active') -->
 *
 * @example
 * <!-- value is null or undefined -->
 * {{ status | concatI18nKey:'app.status' }}
 * <!-- results in: '-' -->
 */
@Pipe({name: 'concatI18nKey'})
export class ConcatI18nKey implements PipeTransform {
  transform(value: unknown, prefix: string, fallback = '-'): string {
    return value != null ? `${prefix}.${value}` : fallback;
  }
}

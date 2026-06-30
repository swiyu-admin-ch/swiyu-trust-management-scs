import {fromLocalizedMap, toLocalizedMap} from './localized-map.util';

describe('localized map utilities', () => {
  it('builds localized map from default value and translations', () => {
    expect(
      toLocalizedMap('Default Name', [
        {name: 'Nom FR', language: 'fr-CH'},
        {name: '  ', language: 'de-CH'}
      ])
    ).toEqual({
      default: 'Default Name',
      'fr-CH': 'Nom FR'
    });
  });

  it('splits localized map into default value and translations', () => {
    expect(
      fromLocalizedMap({
        default: 'Default Name',
        'de-CH': 'Deutsch',
        'fr-CH': 'Français'
      })
    ).toEqual({
      defaultValue: 'Default Name',
      translations: [
        {name: 'Deutsch', language: 'de-CH'},
        {name: 'Français', language: 'fr-CH'}
      ]
    });
  });
});

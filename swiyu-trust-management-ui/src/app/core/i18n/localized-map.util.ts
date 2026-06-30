/** Key used by CBS for the fallback value in localized text maps. */
export const DEFAULT_LOCALIZED_TEXT_KEY = 'default';

export type LocalizedTextMap = Record<string, string>;
export interface LocalizedTextTranslation {
  name: string;
  language: string;
}

export function toLocalizedMap(
  defaultValue: string,
  translations: {name?: string | null; language?: string | null}[]
): LocalizedTextMap {
  const trimmedDefault = defaultValue.trim();
  const map: LocalizedTextMap = {
    [DEFAULT_LOCALIZED_TEXT_KEY]: trimmedDefault
  };

  for (const entry of translations) {
    const {name, language} = entry;
    if (language && name && name.trim()) {
      map[language] = name;
    }
  }

  return map;
}

export function fromLocalizedMap(map: LocalizedTextMap | null | undefined): {
  defaultValue: string;
  translations: LocalizedTextTranslation[];
} {
  if (!map) {
    return {defaultValue: '', translations: []};
  }

  const defaultValue = map[DEFAULT_LOCALIZED_TEXT_KEY] ?? '';
  const translations = Object.entries(map)
    .filter(([key, value]) => key !== DEFAULT_LOCALIZED_TEXT_KEY && Boolean(value?.trim()))
    .flatMap(([language, name]) => {
      if (language) {
        return [{name, language}];
      }
      return [];
    });

  return {defaultValue, translations};
}

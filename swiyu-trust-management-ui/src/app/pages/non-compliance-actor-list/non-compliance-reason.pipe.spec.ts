import {provideZoneChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {provideTranslateService} from '@ngx-translate/core';
import {NonCompliantReasonText} from '../../api/generated';
import {NonComplianceReasonPipe} from './non-compliance-reason.pipe';

describe('NonComplianceReasonPipe', () => {
  let pipe: NonComplianceReasonPipe;

  const reason: NonCompliantReasonText = {
    reasonDe: 'de text',
    reasonEn: 'en text',
    reasonFr: 'fr text',
    reasonIt: 'it text',
    reasonRm: 'rm text'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideZoneChangeDetection({eventCoalescing: true}), provideTranslateService()]
    }).compileComponents();
    pipe = TestBed.runInInjectionContext(() => new NonComplianceReasonPipe());
  });

  it('returns empty for undefined reason', () => {
    expect(pipe.transform(undefined)).toBe('');
  });

  it('falls back through de, en, fr, it, rm when user language is missing', () => {
    // Use a language not present in the reason (e.g. zh) to force the static fallback order.
    (pipe as unknown as {localizeService: {currentLang: () => string}}).localizeService.currentLang = () => 'zh';
    expect(pipe.transform(reason)).toBe('de text');
  });

  it('returns the user language when available', () => {
    (pipe as unknown as {localizeService: {currentLang: () => string}}).localizeService.currentLang = () => 'en';
    expect(pipe.transform(reason)).toBe('en text');
  });

  it('skips empty languages in the priority order', () => {
    const partial: NonCompliantReasonText = {reasonEn: '', reasonFr: 'fr text'};
    (pipe as unknown as {localizeService: {currentLang: () => string}}).localizeService.currentLang = () => 'zh';
    expect(pipe.transform(partial)).toBe('fr text');
  });
});

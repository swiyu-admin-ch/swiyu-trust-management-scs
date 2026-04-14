import {inject, Injectable} from '@angular/core';
import {ObMasterLayoutConfig} from '@oblique/oblique';
import {AppConfigService} from './app-config.service';

/**
 * Loads the app configuration from backend before bootstrapping the application.
 */
export function appInitializer(): Promise<unknown> {
  return ((appInitializerService: AppInitializerService) => {
    return () => {
      return appInitializerService.initialize();
    };
  })(inject(AppInitializerService))();
}

@Injectable({
  providedIn: 'root'
})
export class AppInitializerService {
  private readonly appConfigService = inject(AppConfigService);
  private readonly masterLayoutConfig = inject(ObMasterLayoutConfig);

  public initialize(): Promise<unknown> {
    this.initObliqueLanguageSelectItems();
    this.initObliqueLanguageFromUrl();
    return this.appConfigService.loadAppConfig();
  }

  /**
   * Oblique 14 does not support initializing the language select items in app.component.ts.
   */
  private initObliqueLanguageSelectItems() {
    this.masterLayoutConfig.locale.locales = ['de-CH', 'fr-CH', 'it-CH'];
  }

  /**
   * Extract language before oblique is loaded to prevent simultaneous language change
   */
  private initObliqueLanguageFromUrl() {
    // When application is called from ePortal the language is passed as query param
    const param = new URLSearchParams(window.location.search).get('lang');
    if (param) {
      localStorage.setItem('oblique_lang', param);
    }
  }
}

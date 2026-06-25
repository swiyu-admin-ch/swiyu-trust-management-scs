import {APP_BASE_HREF, registerLocaleData} from '@angular/common';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import localeDECH from '@angular/common/locales/de-CH';
import localeENCH from '@angular/common/locales/en';
import localeFRCH from '@angular/common/locales/fr-CH';
import localeITCH from '@angular/common/locales/it-CH';
import {ApplicationConfig, importProvidersFrom, provideAppInitializer, provideZoneChangeDetection} from '@angular/core';
import {bootstrapApplication} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {OB_BANNER, ObHttpApiInterceptor, provideObliqueConfiguration} from '@oblique/oblique';
import {provideOAuthClient} from 'angular-oauth2-oidc';
import {AppComponent} from './app/app.component';
import {routes} from './app/app.routes';

import {MAT_DATE_LOCALE} from '@angular/material/core';
import {BASE_PATH, Configuration, provideApi} from './app/api/generated';
import {AppConfigService} from './app/core/appconfig/app-config.service';
import {appInitializer} from './app/core/appconfig/app-initializer.service';

registerLocaleData(localeDECH);
registerLocaleData(localeFRCH);
registerLocaleData(localeITCH);
registerLocaleData(localeENCH);

const applicationConfig: ApplicationConfig = {
  providers: [
    provideAppInitializer(() => appInitializer()),
    provideObliqueConfiguration({
      accessibilityStatement: {
        applicationName: 'swiyu Trust Management',
        createdOn: new Date('2025-10-29'),
        conformity: 'full',
        applicationOperator:
          'Replace me with the name and address of the federal office that exploit this application, HTML is permitted',
        contact: [{/* at least 1 email or phone number has to be provided */ email: ''}]
      }
    }),
    {
      provide: OB_BANNER,
      deps: [AppConfigService],
      useFactory: (appConfigService: AppConfigService): object => {
        return {text: appConfigService.banner};
      }
    },
    {provide: HTTP_INTERCEPTORS, useClass: ObHttpApiInterceptor, multi: true},
    {provide: MAT_DATE_LOCALE, useValue: 'de-CH'}, // global datepicker locale - we always want the date format dd.MM.yyyy
    {provide: APP_BASE_HREF, useValue: '/ui'},
    {provide: BASE_PATH, useValue: ''},
    provideApi(new Configuration({})),
    provideZoneChangeDetection({eventCoalescing: true}),
    provideHttpClient(),
    provideRouter(routes, withComponentInputBinding()),
    provideObliqueConfiguration({
      accessibilityStatement: {
        applicationName: 'swiyu Trust Infrastructure',
        createdOn: new Date('2025-10-29'),
        conformity: 'full',
        applicationOperator:
          'Replace me with the name and address of the federal office that exploit this application, HTML is permitted',
        contact: [{/* at least 1 email or phone number has to be provided */ email: ''}]
      }
    }),
    importProvidersFrom([BrowserAnimationsModule]),
    provideHttpClient(withInterceptorsFromDi()),
    provideOAuthClient({
      resourceServer: {
        allowedUrls: ['/ui-api'],
        sendAccessToken: true
      }
    })
  ]
};

bootstrapApplication(AppComponent, applicationConfig).catch(e => console.error(e));

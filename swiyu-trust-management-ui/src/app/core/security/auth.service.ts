import {DestroyRef, inject, Injectable} from '@angular/core';
import {AuthConfig, OAuthService} from 'angular-oauth2-oidc';

import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NavigationEnd, Router} from '@angular/router';
import {distinctUntilChanged, filter, from, map, skip, tap} from 'rxjs';
import {AppConfig} from '../../api/generated';
import {AppConfigService} from '../appconfig/app-config.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private readonly router = inject(Router);
  private readonly appConfigService = inject(AppConfigService);

  private destroyRef = inject(DestroyRef);

  constructor() {
    this.reloadPageOnAccessTokenStateChanges();
  }

  get isLoggedIn() {
    return this.oauthService.hasValidAccessToken();
  }

  configureFlowAndLogin() {
    const appConfig = this.appConfigService.appConfig();
    if (appConfig) {
      this.oauthService.configure(createAuthConfig(appConfig));

      this.router.events
        .pipe(
          filter((e): e is NavigationEnd => e instanceof NavigationEnd),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe(value => {
          if (value.urlAfterRedirects != `/logged-out` && !this.isLoggedIn) {
            this.initLoginFlow(appConfig?.authConfig.useSilentRefresh);
          }
        });
    }
  }

  login() {
    const appConfig = this.appConfigService.appConfig();
    if (!this.isLoggedIn) {
      this.initLoginFlow(appConfig?.authConfig.useSilentRefresh);
    }
  }

  logout() {
    const login$ = from(this.oauthService.loadDiscoveryDocumentAndTryLogin());
    login$.subscribe(() => {
      if (this.isLoggedIn) {
        this.oauthService.logOut();
      }
    });
  }

  private initLoginFlow(withSilentRefresh: boolean | undefined): void {
    const login$ = from(this.oauthService.loadDiscoveryDocumentAndTryLogin());
    if (withSilentRefresh) {
      login$.pipe(tap(() => this.oauthService.setupAutomaticSilentRefresh()));
    }
    login$.subscribe(() => {
      if (!this.isLoggedIn) {
        this.oauthService.initLoginFlow();
      }
    });
  }

  /**
   * Workaround since API calls don't work right after login.
   */
  private reloadPageOnAccessTokenStateChanges() {
    this.oauthService.events
      .pipe(
        map(() => this.oauthService.hasValidAccessToken()),
        // Only emit when the validity state changes
        distinctUntilChanged(),
        // skip the first iteration as we go from unknown to false
        skip(1),
        tap(() => window.location.reload())
      )
      .subscribe();
  }
}

function createAuthConfig(appConfig: AppConfig): AuthConfig {
  const backendAuthConfig = appConfig.authConfig;
  if (!backendAuthConfig) {
    throw new Error(
      'Failed to configure authentication due to missing app config. Did the backend return a valid config?'
    );
  }

  return {
    requireHttps: backendAuthConfig.requireHttps,
    responseType: backendAuthConfig.responseType,
    clientId: backendAuthConfig.clientId,
    scope: backendAuthConfig.scope,
    issuer: backendAuthConfig.issuer,
    redirectUri: `${window.location.origin}/ui`,
    redirectUriAsPostLogoutRedirectUriFallback: true,
    postLogoutRedirectUri: `${window.location.origin}/ui/logged-out`,
    preserveRequestedRoute: true,
    strictDiscoveryDocumentValidation: false,
    useSilentRefresh: backendAuthConfig.useSilentRefresh,
    silentRefreshRedirectUri: `${window.location.origin}/assets/auth/silent-refresh.html`
  };
}

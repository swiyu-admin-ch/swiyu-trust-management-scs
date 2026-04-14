import {inject, Injectable, signal} from '@angular/core';
import {catchError, firstValueFrom, of} from 'rxjs';
import {AppConfig} from '../../api/generated';
import {FrontendConfigurationApi} from '../../api/generated/api/frontend-configuration-api';

@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  private readonly api = inject(FrontendConfigurationApi);

  readonly appConfig = signal<AppConfig | undefined>(undefined);

  async loadAppConfig() {
    const config = await firstValueFrom(this.fetchAppConfig());
    this.appConfig.set(config);
  }

  get banner(): string | undefined {
    return this.appConfig()?.environment;
  }

  get version(): string | undefined {
    return this.appConfig()?.version;
  }

  private fetchAppConfig() {
    return this.api.getConfiguration().pipe(
      catchError(error => {
        console.error(`failed to load app config`, error);
        return of(undefined);
      })
    );
  }
}

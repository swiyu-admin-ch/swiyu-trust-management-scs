import {Component, inject, OnInit} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {TranslateModule} from '@ngx-translate/core';
import {
  ObButtonDirective,
  ObButtonModule,
  ObDocumentMetaModule,
  ObDocumentMetaService,
  ObHttpApiInterceptorConfig,
  ObMasterLayoutConfig,
  ObMasterLayoutModule,
  ObOffCanvasModule
} from '@oblique/oblique';
import {AppConfigService} from './core/appconfig/app-config.service';
import {AuthService} from './core/security/auth.service';
import {SidepanelHeaderComponent} from './pages/trust-onboarding-task-detail/sidepanel/header/header.component';
import {SidepanelComponent} from './pages/trust-onboarding-task-detail/sidepanel/sidepanel.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [
    ObMasterLayoutModule,
    TranslateModule,
    ObOffCanvasModule,
    SidepanelComponent,
    SidepanelHeaderComponent,
    ObDocumentMetaModule,
    MatButtonModule,
    ObButtonModule,
    ObButtonDirective,
    MatIconModule
  ]
})
export class AppComponent implements OnInit {
  readonly appConfigService = inject(AppConfigService);
  readonly authService = inject(AuthService);
  private readonly config: ObMasterLayoutConfig = inject(ObMasterLayoutConfig);
  private readonly meta = inject(ObDocumentMetaService);
  private readonly interceptorConfig = inject(ObHttpApiInterceptorConfig);

  constructor() {
    this.config.layout.hasMaxWidth = true;
    this.config.layout.hasOffCanvas = true;
    this.config.header.serviceNavigation.displayLanguages = true;
    this.meta.titleSuffix = 'app.name';
    this.meta.titleSeparator = ' | ';
    this.interceptorConfig.api.spinner = true;
    this.interceptorConfig.api.url = 'ui-api';
  }

  ngOnInit(): void {
    this.authService.configureFlowAndLogin();
  }
}

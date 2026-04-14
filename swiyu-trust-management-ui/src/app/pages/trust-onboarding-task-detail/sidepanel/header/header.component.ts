import {Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TranslateService} from '@ngx-translate/core';
import {TrustOnboardingTaskAction} from '../../../../api/generated';
import {PanelData, SidepanelService} from '../sidepanel.service';

@Component({
  selector: 'app-sidepanel-header',
  imports: [],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class SidepanelHeaderComponent {
  private readonly sidepanelService = inject(SidepanelService);
  private translateService = inject(TranslateService);

  panelData = signal<PanelData | null>(null);
  title = signal<string | null>(null);

  constructor() {
    this.sidepanelService.panelData$.pipe(takeUntilDestroyed()).subscribe(data => {
      this.panelData.set(data);
      this.title.set(this.resolvePanelTitle(data?.action));
    });
  }

  private resolvePanelTitle(action?: TrustOnboardingTaskAction) {
    switch (action) {
      case TrustOnboardingTaskAction.Approve:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.approve');
      case TrustOnboardingTaskAction.Reject:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.reject');
      case TrustOnboardingTaskAction.RequestMoreInformation:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.request-more-information');
      case TrustOnboardingTaskAction.AddInternalNote:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.add-note');
      default:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.submit');
    }
  }
}

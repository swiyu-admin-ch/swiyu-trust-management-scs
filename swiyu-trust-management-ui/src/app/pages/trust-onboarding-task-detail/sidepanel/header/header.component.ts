import {Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TranslateService} from '@ngx-translate/core';
import {TaskAction} from '../../../../api/generated';
import {PanelData, SidepanelService} from '../sidepanel.service';

@Component({
  selector: 'app-sidepanel-header',
  imports: [],
  templateUrl: './header.component.html'
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

  private resolvePanelTitle(action?: TaskAction) {
    switch (action) {
      case TaskAction.Approve:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.approve');
      case TaskAction.Reject:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.reject');
      case TaskAction.RequestMoreInformation:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.request-more-information');
      case TaskAction.AddInternalNote:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.add-note');
      default:
        return this.translateService.instant('app.trust-onboarding-task.sidepanel.submit');
    }
  }
}

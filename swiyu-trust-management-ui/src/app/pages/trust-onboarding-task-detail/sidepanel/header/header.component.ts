import {Component, inject} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {_, TranslateService} from '@ngx-translate/core';
import {map, merge, of, switchMap} from 'rxjs';
import {TaskAction} from '../../../../api/generated';
import {BusinessPartnerDeletePanelService} from '../../../business-partner-list/delete-panel/business-partner-delete-panel.service';
import {SidepanelService} from '../sidepanel.service';

/** Panel title per task action. {@code AssignSelf} is handled inline in the list and never opens the panel. */
const TASK_ACTION_TITLE_KEYS: Partial<Record<TaskAction, string>> = {
  [TaskAction.Approve]: _('app.trust-onboarding-task.sidepanel.approve'),
  [TaskAction.Reject]: _('app.trust-onboarding-task.sidepanel.reject'),
  [TaskAction.RequestMoreInformation]: _('app.trust-onboarding-task.sidepanel.request-more-information'),
  [TaskAction.AddInternalNote]: _('app.trust-onboarding-task.sidepanel.add-note')
};

const DELETE_PARTNER_TITLE_KEY = _('businessPartner.delete.title');

@Component({
  selector: 'app-sidepanel-header',
  imports: [],
  templateUrl: './header.component.html'
})
export class SidepanelHeaderComponent {
  private readonly sidepanelService = inject(SidepanelService);
  private readonly deletePanelService = inject(BusinessPartnerDeletePanelService);
  private readonly translateService = inject(TranslateService);

  /**
   * Both panels share the single off-canvas, so the title follows whichever of them emitted last. Resolved through
   * {@code stream} rather than {@code instant}: this component is created while the app bootstraps, before the
   * translations are loaded, and the title has to follow a language switch while the panel is open.
   */
  readonly title = toSignal(
    merge(
      this.sidepanelService.panelData$.pipe(map(data => (data ? TASK_ACTION_TITLE_KEYS[data.action] : undefined))),
      this.deletePanelService.partner$.pipe(map(partner => (partner ? DELETE_PARTNER_TITLE_KEY : undefined)))
    ).pipe(switchMap(titleKey => (titleKey ? this.translateService.stream(titleKey) : of('')))),
    {initialValue: ''}
  );
}

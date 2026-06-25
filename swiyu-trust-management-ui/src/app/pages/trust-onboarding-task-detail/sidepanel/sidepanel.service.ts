import {Injectable, inject} from '@angular/core';
import {ObOffCanvasService} from '@oblique/oblique';
import {BehaviorSubject, Subject} from 'rxjs';
import {TrustOnboardingTask, TrustOnboardingTaskAction} from '../../../api/generated';

export interface PanelData {
  task: TrustOnboardingTask;
  action: TrustOnboardingTaskAction;
}

@Injectable({providedIn: 'root'})
export class SidepanelService {
  private readonly obOffCanvasService = inject(ObOffCanvasService);

  private panelDataSubject = new BehaviorSubject<PanelData | null>(null);
  private reloadTrigger = new Subject<void>();
  reload$ = this.reloadTrigger.asObservable();
  panelData$ = this.panelDataSubject.asObservable();

  openPanel(data: PanelData) {
    this.panelDataSubject.next(data);
    this.obOffCanvasService.open = true;
  }

  closePanel() {
    this.clearPanel();
    this.triggerReload();
  }

  addNote(task: TrustOnboardingTask) {
    this.openPanel({action: TrustOnboardingTaskAction.AddInternalNote, task});
  }

  approve(task: TrustOnboardingTask) {
    this.openPanel({action: TrustOnboardingTaskAction.Approve, task});
  }

  reject(task: TrustOnboardingTask) {
    this.openPanel({action: TrustOnboardingTaskAction.Reject, task});
  }

  requestMoreInformation(task: TrustOnboardingTask) {
    this.openPanel({action: TrustOnboardingTaskAction.RequestMoreInformation, task});
  }

  private clearPanel() {
    this.obOffCanvasService.open = false;
    this.panelDataSubject.next(null);
  }

  private triggerReload() {
    this.reloadTrigger.next();
  }
}

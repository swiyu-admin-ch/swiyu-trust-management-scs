import {Injectable, inject} from '@angular/core';
import {ObOffCanvasService} from '@oblique/oblique';
import {BehaviorSubject, Subject} from 'rxjs';
import {TaskAction, TaskListItem, TrustOnboardingTask} from '../../../api/generated';

export interface PanelData {
  task: TrustOnboardingTask | TaskListItem;
  action: TaskAction;
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

  addNote(task: TrustOnboardingTask | TaskListItem) {
    this.openPanel({action: TaskAction.AddInternalNote, task});
  }

  approve(task: TrustOnboardingTask | TaskListItem) {
    this.openPanel({action: TaskAction.Approve, task});
  }

  reject(task: TrustOnboardingTask | TaskListItem) {
    this.openPanel({action: TaskAction.Reject, task});
  }

  requestMoreInformation(task: TrustOnboardingTask) {
    this.openPanel({action: TaskAction.RequestMoreInformation, task});
  }

  private clearPanel() {
    this.obOffCanvasService.open = false;
    this.panelDataSubject.next(null);
  }

  private triggerReload() {
    this.reloadTrigger.next();
  }
}

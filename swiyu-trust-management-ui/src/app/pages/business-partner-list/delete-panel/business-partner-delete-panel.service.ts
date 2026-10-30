import {inject, Injectable} from '@angular/core';
import {ObOffCanvasService} from '@oblique/oblique';
import {BehaviorSubject, Subject} from 'rxjs';
import {BusinessPartnerViewListItem} from '../../../api/generated';

/**
 * Drives the delete-partner sidepanel. Kept separate from the task {@code SidepanelService}: that one is keyed on a
 * task and its {@code TaskAction}, while this panel starts from a partner that has no task yet.
 */
@Injectable({providedIn: 'root'})
export class BusinessPartnerDeletePanelService {
  private readonly obOffCanvasService = inject(ObOffCanvasService);

  private readonly partnerSubject = new BehaviorSubject<BusinessPartnerViewListItem | null>(null);
  private readonly createdTrigger = new Subject<void>();

  readonly partner$ = this.partnerSubject.asObservable();
  /** Fires after a deletion request was created, so the list can reload. */
  readonly created$ = this.createdTrigger.asObservable();

  open(partner: BusinessPartnerViewListItem): void {
    this.partnerSubject.next(partner);
    this.obOffCanvasService.open = true;
  }

  close(): void {
    this.obOffCanvasService.open = false;
    this.partnerSubject.next(null);
  }

  closeAfterCreate(): void {
    this.close();
    this.createdTrigger.next();
  }
}

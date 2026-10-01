import {Clipboard} from '@angular/cdk/clipboard';
import {DatePipe} from '@angular/common';
import {Component, computed, DestroyRef, inject, input, signal} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {RouterLink} from '@angular/router';
import {_, TranslatePipe, TranslateService} from '@ngx-translate/core';
import {
  ObAlertComponent,
  ObButtonModule,
  ObColumnLayoutModule,
  ObDocumentMetaService,
  ObENotificationType,
  ObNotificationService,
  WINDOW
} from '@oblique/oblique';
import {catchError, combineLatest, EMPTY, finalize, switchMap, take, tap} from 'rxjs';
import {NonCompliantActor, NonCompliantActorApi} from '../../api/generated';

@Component({
  selector: 'app-non-compliance-detail',
  imports: [
    TranslatePipe,
    ObAlertComponent,
    ObColumnLayoutModule,
    MatButtonModule,
    ObButtonModule,
    MatIconModule,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    MatProgressSpinnerModule,
    RouterLink,
    DatePipe
  ],
  templateUrl: './non-compliance-detail.component.html',
  styleUrl: './non-compliance-detail.component.scss'
})
export class NonComplianceDetailComponent {
  private readonly api = inject(NonCompliantActorApi);
  private readonly notificationService = inject(ObNotificationService);
  private readonly translateService = inject(TranslateService);
  private readonly metaService = inject(ObDocumentMetaService);
  private readonly clipboard = inject(Clipboard);
  private readonly window = inject(WINDOW);
  private readonly destroyRef = inject(DestroyRef);

  actorId = input.required<string>();
  actor = signal({} as NonCompliantActor);
  notFoundError = signal(false);
  readonly loading = signal(false);

  readonly reasonLanguages = computed(() => {
    const reason = this.actor().reason;
    if (!reason) {
      return [];
    }
    return [
      {key: 'de', labelKey: _('nonCompliance.detail.fields.reason.de.label'), value: reason.reasonDe},
      {key: 'fr', labelKey: _('nonCompliance.detail.fields.reason.fr.label'), value: reason.reasonFr},
      {key: 'it', labelKey: _('nonCompliance.detail.fields.reason.it.label'), value: reason.reasonIt},
      {key: 'en', labelKey: _('nonCompliance.detail.fields.reason.en.label'), value: reason.reasonEn},
      {key: 'rm', labelKey: _('nonCompliance.detail.fields.reason.rm.label'), value: reason.reasonRm}
    ];
  });

  constructor() {
    toObservable(this.actorId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        switchMap(id => this.loadActor(id))
      )
      .subscribe();
  }

  shareLink() {
    const link = this.window.location.toString();
    combineLatest([
      this.translateService.get('app.trust-onboarding-task.actions.share-link.notification.title'),
      this.translateService.get('app.trust-onboarding-task.actions.share-link.notification.message', {link: link})
    ])
      .pipe(take(1))
      .subscribe(([title, message]) => {
        this.notificationService.send({title, message}, ObENotificationType.INFO);
      });
    this.clipboard.copy(link);
  }

  private loadActor(id: string) {
    this.loading.set(true);
    return this.api.getNonCompliantActor({nonCompliantActorId: id}).pipe(
      tap(actor => {
        this.actor.set(actor);
        this.notFoundError.set(false);
        this.metaService.setTitle(actor.did || actor.businessPartnerId || '');
      }),
      catchError(err => {
        this.actor.set({} as NonCompliantActor);
        if (err.status === 404) {
          this.notFoundError.set(true);
          this.metaService.setTitle('app.menu.nonComplianceActors');
        }
        return EMPTY;
      }),
      finalize(() => this.loading.set(false))
    );
  }
}

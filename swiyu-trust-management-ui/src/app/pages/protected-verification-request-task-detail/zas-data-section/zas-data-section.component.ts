import {Component, inject, input, output, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {TranslatePipe} from '@ngx-translate/core';
import {ObAlertComponent, ObButtonModule} from '@oblique/oblique';
import {finalize, map, switchMap, take} from 'rxjs';
import {TrustOnboardingTaskApi, ZasData} from '../../../api/generated';

@Component({
  selector: 'app-zas-data-section',
  standalone: true,
  templateUrl: './zas-data-section.component.html',
  styleUrl: './zas-data-section.component.scss',
  imports: [TranslatePipe, MatButtonModule, ObButtonModule, MatIconModule, MatProgressSpinnerModule, ObAlertComponent]
})
export class ZasDataSectionComponent {
  private readonly api = inject(TrustOnboardingTaskApi);

  taskId = input.required<string>();
  opened = output<void>();

  zasData = signal<ZasData | null>(null);
  loading = signal(false);
  error = signal(false);

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.api
      .getProtectedVerificationRequestTaskZasData({taskId: this.taskId()})
      .pipe(
        // Fetching is a side-effect-free GET; marking the data as reviewed - which gates approve/reject - is a
        // separate, explicit action, done only once the data has actually been fetched and is about to be shown.
        switchMap(zasData =>
          this.api.markProtectedVerificationRequestTaskZasDataReviewed({taskId: this.taskId()}).pipe(map(() => zasData))
        ),
        take(1),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: zasData => {
          this.zasData.set(zasData);
          this.opened.emit();
        },
        error: () => {
          this.zasData.set(null);
          this.error.set(true);
        }
      });
  }
}

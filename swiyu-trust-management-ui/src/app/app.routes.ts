import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {LogoutComponent} from './pages/logout/logout.component';
import {TrustAddDidTaskDetailComponent} from './pages/trust-add-did-task-detail/trust-add-did-task-detail.component';
import {TrustOnboardingTaskDetailComponent} from './pages/trust-onboarding-task-detail/trust-onboarding-task-detail.component';
import {TrustOnboardingTaskListComponent} from './pages/trust-onboarding-task-list/trust-onboarding-task-list.component';

export const routes: Routes = [
  {path: 'logged-out', component: LogoutComponent},
  {path: 'home', component: HomeComponent},
  {
    path: 'tasks',
    children: [
      {path: '', component: TrustOnboardingTaskListComponent},
      {path: ':taskId/add-did', component: TrustAddDidTaskDetailComponent},
      {path: ':taskId', component: TrustOnboardingTaskDetailComponent}
    ]
  },
  {path: '**', redirectTo: 'tasks', pathMatch: 'full'}
];

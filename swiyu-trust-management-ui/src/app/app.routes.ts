import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {LogoutComponent} from './pages/logout/logout.component';
import {NonComplianceActorCreateComponent} from './pages/non-compliance-actor-create/non-compliance-actor-create.component';
import {NonComplianceActorListComponent} from './pages/non-compliance-actor-list/non-compliance-actor-list.component';
import {NonComplianceDetailComponent} from './pages/non-compliance-detail/non-compliance-detail.component';
import {ProtectedVerificationRequestTaskDetailComponent} from './pages/protected-verification-request-task-detail/protected-verification-request-task-detail.component';
import {TaskListComponent} from './pages/task-list/task-list.component';
import {TrustAddDidTaskDetailComponent} from './pages/trust-add-did-task-detail/trust-add-did-task-detail.component';
import {TrustOnboardingTaskDetailComponent} from './pages/trust-onboarding-task-detail/trust-onboarding-task-detail.component';

export const routes: Routes = [
  {path: 'logged-out', component: LogoutComponent},
  {path: 'home', component: HomeComponent},
  {
    path: 'tasks',
    children: [
      {path: '', component: TaskListComponent},
      {path: ':taskId/add-did', component: TrustAddDidTaskDetailComponent},
      {
        path: ':taskId/protected-verification-request',
        component: ProtectedVerificationRequestTaskDetailComponent
      },
      {path: ':taskId', component: TrustOnboardingTaskDetailComponent}
    ]
  },
  {path: 'non-compliance-actors', component: NonComplianceActorListComponent},
  {path: 'non-compliance-actors/create', component: NonComplianceActorCreateComponent},
  {path: 'non-compliance-actors/:actorId', component: NonComplianceDetailComponent},
  {path: '**', redirectTo: 'tasks', pathMatch: 'full'}
];

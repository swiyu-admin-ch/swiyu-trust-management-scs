import {Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {TranslatePipe} from '@ngx-translate/core';

@Component({
  selector: 'app-logout',
  imports: [TranslatePipe, MatCardModule, MatIconModule],
  templateUrl: './logout.component.html'
})
export class LogoutComponent {}

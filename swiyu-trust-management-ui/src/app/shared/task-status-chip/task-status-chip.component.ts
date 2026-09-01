import {Component, input} from '@angular/core';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {TranslatePipe} from '@ngx-translate/core';
import {TaskStatus} from '../../api/generated';

@Component({
  selector: 'app-task-status-chip',
  imports: [MatChip, MatChipSet, TranslatePipe],
  templateUrl: './task-status-chip.component.html'
})
export class TaskStatusChipComponent {
  readonly TaskStatus = TaskStatus;
  status = input.required<TaskStatus>();
}

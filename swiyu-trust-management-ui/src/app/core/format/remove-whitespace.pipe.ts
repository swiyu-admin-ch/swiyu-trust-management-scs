import {Pipe, PipeTransform} from '@angular/core';

@Pipe({
  name: 'removeWhitespace'
})
export class RemoveWhitespacePipe implements PipeTransform {
  transform(value: string): string {
    return value ? value.replace(/\s+/g, '') : '';
  }
}

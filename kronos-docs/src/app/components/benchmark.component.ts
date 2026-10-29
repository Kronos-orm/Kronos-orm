import {Component} from '@angular/core';
import {MarkdownComponent} from 'ngx-markdown';

@Component({
    selector: 'kronos-benchmark-markdown',
    imports: [MarkdownComponent],
    template: `
        <markdown class="benchmark-report markdown-body"
                  [src]="'https://raw.githubusercontent.com/Kronos-orm/orm-benchmark-project/result/readme.md'"
                  />
  `,
    standalone: true,
    styleUrl: './benchmark.component.css',
})
export class BenchmarkComponent {}

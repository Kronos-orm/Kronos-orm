import {
    Component,
} from '@angular/core';
import {SharedModule} from "../shared.module";
import {MarkdownComponent} from "ngx-markdown";

@Component({
    selector: 'kronos-benchmark-markdown',
    imports: [
        SharedModule,
        MarkdownComponent
    ],
    template: `
        <markdown class="markdown-body"
                  [src]="'https://raw.githubusercontent.com/Kronos-orm/orm-benchmark-project/result/readme.md'"
                  lineNumbers/>
  `,
    standalone: true,
    styles: [
        `
            :host {
                text-align: center;
                display: block;
                width: 100%;
                background: rgba(20, 0, 100, 0.4);
                padding: 20px;
                border-radius: 10px;
            }
            
            :host ::ng-deep p:has(img) {
                padding: 10px;
                background: rgba(20, 0, 100, 0.4);
                border-radius: 10px;
                text-align: center;
            }
        `
    ]
})
export class BenchmarkComponent {}

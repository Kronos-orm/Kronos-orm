import {Component, EventEmitter, Input, Output,} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";

export interface ListItem {
    label: string;
    leftIcon?: string;
    rightIcon?: string;
    [key: string]: any;
}

@Component({
    selector: 'ui-list',
    imports: [
        SharedModule,
        AnimateOnScrollModule
    ],
    template: `
        <ul class="list-none m-0 p-0 w-48">
            @for (item of list; track $index) {
                <a class="block p-2 rounded-border hover:bg-emphasis w-full cursor-pointer flex"
                   (click)="onClick.emit(item)">
                    <div *ngIf="item.leftIcon" class="mr-2 text-white flex-1 text-right inline-block">
                        <span class="pi pi-globe"></span>
                    </div>
                    <span class="font-bold text-white flex-1">{{ item.label }}</span>
                    <div *ngIf="item.rightIcon" class="ml-2 text-white flex-1 text-right inline-block">
                        <span class="pi pi-globe"></span>
                    </div>
                </a>
            }
        </ul>
    `,
    standalone: true,
    styles: [`
      a {
        text-decoration: none;
      }
    `]
})
export class UIListComponent {
    @Input() list: ListItem[];
    @Output() onClick = new EventEmitter<ListItem>();
}

import {Component, ElementRef, EventEmitter, ViewChild,} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {NG_DOC_ROUTING} from "@ng-doc/generated";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {AppService} from "../app.service";
import {Router} from "@angular/router";
import {Dialog, DialogModule} from "primeng/dialog";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
    selector: 'document-wiki',
    imports: [
        SharedModule,
        AnimateOnScrollModule,
        DialogModule,
        TranslocoPipe
    ],
    template: `
        <p-dialog #dialog
                  styleClass="wiki"
                  [closable]="false"
                  [modal]="true"
                  [(visible)]="visible"
                  [style]="{ width: '70vw', height: '70vh' }"
                  [blockScroll]="false"
                  position="center"
                  [appendTo]="elementRef.nativeElement">
            <div class="flex flex-column gap-3 w-full h-full">
                <iframe *ngIf="safeUrl" class="w-full h-full" [src]="safeUrl" frameborder="0"></iframe>
            </div>
            <ng-template pTemplate="header">
                <div class="w-full flex justify-content-end pr-2">
                    <p-button pRipple link styleClass="text-gray-500" tooltipPosition="top"
                              [style]="{transform: 'scaleX(-1)'}"
                              [pTooltip]="'JUMP_TO_WIKI' | transloco" (click)="expand()" icon="pi pi-clone"/>
                    <p-button pRipple link styleClass="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'MAXIMIZE' | transloco" class="hidden md:block" *ngIf="!dialog.maximized"
                              (click)="dialog.maximize()" icon="pi pi-window-maximize"/>
                    <p-button pRipple link styleClass="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'MAXIMIZED' | transloco" class="hidden md:block" *ngIf="dialog.maximized"
                              (click)="dialog.maximize()" icon="pi pi-window-minimize"/>
                    <p-button pRipple link styleClass="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'OPEN_IN_NEW_WINDOW' | transloco" (click)="jump()"
                              icon="pi pi-external-link"/>
                    <p-button pRipple link styleClass="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'CLOSE_WIKI_DIALOG' | transloco" (click)="hide()" icon="pi pi-times"/>
                </div>
            </ng-template>
        </p-dialog>
    `,
    standalone: true,
    styles: []
})
export class WikiComponent {
    routing = NG_DOC_ROUTING;
    url: string;
    safeUrl: SafeUrl;
    visible = false;

    constructor(
        private appService: AppService,
        private sanitizer: DomSanitizer,
        public router: Router,
        protected elementRef: ElementRef
    ) {
        window.onWikiChange = new EventEmitter();
        window.onWikiChange.subscribe(({id, anchor}) => {
            this.url = `/documentation/${this.appService.language}/concept/` +
                this.routing
                    .filter(item => item.path.startsWith(this.appService.language))
                    .map(item => item.children)
                    .flat()
                    .find(item => item.path == id).path
            const _anchor = anchor ? `#${anchor}` : ""
            this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                '/#' + this.url + _anchor
            );
            this.visible = true;
            // 如果页面宽度小于900，自动打开全屏模式
            if (window.innerWidth < 900) {
                this.dialogTpl.maximize();
            }
        })
    }

    @ViewChild("dialog", {static: false}) dialogTpl: Dialog;

    hide() {
        this.visible = false;
    }

    async expand() {
        this.visible = false;
        await this.router.navigate([this.url]);
    }

    jump() {
        window.open(window.location.origin + '/#' + this.url, "_blank");
    }
}

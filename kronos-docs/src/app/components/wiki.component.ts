import {ChangeDetectorRef, Component, effect, ElementRef, signal, ViewChild,} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {NG_DOC_ROUTING} from "@ng-doc/generated";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {AppService} from "../app.service";
import {Router} from "@angular/router";
import {Dialog, DialogModule} from "primeng/dialog";
import {TranslocoPipe, TranslocoService} from "@jsverse/transloco";
import {MessageService} from "primeng/api";

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
                  class="wiki"
                  [closable]="false"
                  [modal]="true"
                  [dismissableMask]="true"
                  [(visible)]="visible"
                  [style]="{ width: '70vw', height: '70vh' }"
                  [blockScroll]="false"
                  position="center"
                  [appendTo]="elementRef.nativeElement">
            <div class="flex flex-col gap-4 w-full h-full">
                <iframe *ngIf="safeUrl" class="w-full h-full" [src]="safeUrl" frameborder="0" (load)="syncTheme($event)"></iframe>
            </div>
            <ng-template pTemplate="header">
                <div class="w-full flex justify-end pr-2">
                    <p-button pRipple link class="text-gray-500" tooltipPosition="top"
                              [style]="{transform: 'scaleX(-1)'}"
                              [pTooltip]="'JUMP_TO_WIKI' | transloco" (click)="expand()" icon="pi pi-clone"/>
                    <p-button pRipple link class="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'MAXIMIZE' | transloco" class="hidden md:block" *ngIf="!dialog.maximized"
                              (click)="dialog.maximize()" icon="pi pi-window-maximize"/>
                    <p-button pRipple link class="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'MAXIMIZED' | transloco" class="hidden md:block" *ngIf="dialog.maximized"
                              (click)="dialog.maximize()" icon="pi pi-window-minimize"/>
                    <p-button pRipple link class="text-gray-500" tooltipPosition="top"
                              [pTooltip]="'OPEN_IN_NEW_WINDOW' | transloco" (click)="jump()"
                              icon="pi pi-external-link"/>
                    <p-button pRipple link class="text-gray-500" tooltipPosition="top"
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

    private wikiSignal = signal<{ id: string, anchor: string } | null>(null);

    constructor(
        private appService: AppService,
        private sanitizer: DomSanitizer,
        public router: Router,
        protected elementRef: ElementRef,
        private msg: MessageService,
        private translocoService: TranslocoService,
        private cdr: ChangeDetectorRef,
    ) {
        // 直接把 signal 挂到 window 上，nunjucks onclick 里直接调 .set()
        (window as any).onWikiChange = this.wikiSignal;

        effect(() => {
            const val = this.wikiSignal();
            if (!val) return;
            // 用 setTimeout 推到下一个 microtask，避免在 CD 周期内修改状态
            setTimeout(() => {
                try {
                    this.url = `/documentation/${this.appService.language}/concept/` +
                        this.routing
                            .filter(item => item.path.startsWith(this.appService.language))
                            .map(item => item.children)
                            .flat()
                            .find(item => item.path == val.id).path
                    const _anchor = val.anchor ? `#${val.anchor}` : ""
                    this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                        '/#' + this.url + _anchor
                    );
                    this.visible = true;
                    this.cdr.detectChanges();
                    if (window.innerWidth < 900) {
                        this.dialogTpl.maximize();
                    }
                } catch (e) {
                    this.translocoService.selectTranslate(["COMING_SOON"])
                        .subscribe(([comingSoon]) => {
                            this.msg.add({
                                severity: 'info',
                                detail: comingSoon
                            });
                            this.cdr.detectChanges();
                        })
                }
            });
        });
    }

    @ViewChild("dialog", {static: false}) dialogTpl: Dialog;

    private themeObserver: MutationObserver;
    private currentIframe: HTMLIFrameElement;

    syncTheme(event: Event) {
        const iframe = event.target as HTMLIFrameElement;
        this.currentIframe = iframe;
        try {
            const iframeHtml = iframe.contentDocument?.documentElement;
            if (iframeHtml) {
                iframeHtml.className = document.documentElement.className;
            }
        } catch (e) {}

        // 监听外层主题变化
        if (!this.themeObserver) {
            this.themeObserver = new MutationObserver(() => {
                try {
                    const iframeHtml = this.currentIframe?.contentDocument?.documentElement;
                    if (iframeHtml) {
                        iframeHtml.className = document.documentElement.className;
                    }
                } catch (e) {}
            });
            this.themeObserver.observe(document.documentElement, {
                attributes: true,
                attributeFilter: ['class']
            });
        }
    }

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

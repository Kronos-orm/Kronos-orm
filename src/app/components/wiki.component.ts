import {Component, EventEmitter,} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {NG_DOC_ROUTING} from "@ng-doc/generated";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {AppService} from "../app.service";
import {Router} from "@angular/router";
import {DialogModule} from "primeng/dialog";

@Component({
  selector: 'document-wiki',
  imports: [
    SharedModule,
    AnimateOnScrollModule,
    DialogModule
  ],
  template: `
      <p-dialog styleClass="wiki" [closable]="false" [modal]="false" [(visible)]="visible"
                [style]="{ width: '80vw', height: '80vh' }">
          <div class="flex flex-column gap-3 w-full h-full">
              <iframe style="opacity: 0.7" *ngIf="safeUrl" class="w-full h-full" [src]="safeUrl"
                      frameborder="0"></iframe>
          </div>
        <ng-template pTemplate="header">
          <div class="w-full flex justify-content-end pr-2">
            <p-button pRipple (click)="jump()" icon="pi pi-external-link" link
                      styleClass=" text-gray-500"/>
            <p-button pRipple (click)="expand()" icon="pi pi-expand" link
                      styleClass=" text-gray-500"/>
            <p-button pRipple (click)="hide()" icon="pi pi-times" link
                      styleClass="text-gray-500"/>
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
      public router: Router
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
    })
  }

  hide() {
    this.visible = false;
  }

  async expand() {
    this.visible = false;
    await this.router.navigate([this.url]);
  }

  jump() {
    this.visible = false;
    window.open(window.location.origin + '/#' + this.url, "_blank");
  }
}

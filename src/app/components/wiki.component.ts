import {
  Component, EventEmitter, ViewChild,
} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {NG_DOC_ROUTING} from "@ng-doc/generated";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {OverlayPanel} from "primeng/overlaypanel";
import {AppService} from "../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'document-wiki',
  imports: [
    SharedModule,
    AnimateOnScrollModule
  ],
  template: `
    <p-overlayPanel #wikiPanel styleClass="wiki">
      <div class="flex flex-column gap-3" style="width: 40vw;height: 40vw;min-width: 20rem;min-height: 20rem">
        <iframe *ngIf="url" class="w-full h-full" [src]="url" frameborder="0"></iframe>
      </div>
    </p-overlayPanel>
  `,
  standalone: true,
  styles: []
})
export class WikiComponent {
  routing = NG_DOC_ROUTING;
  url: SafeUrl;

  @ViewChild("wikiPanel", {static: false}) wikiPanel: OverlayPanel;
  constructor(
      private appService: AppService,
      private sanitizer: DomSanitizer) {
    window.onWikiChange = new EventEmitter<{ id: string, event: MouseEvent }>();
    window.onWikiChange.subscribe(({id, event}) => {
      if (this.wikiPanel) {
        this.wikiPanel!!.toggle(event);
        this.url = this.sanitizer.bypassSecurityTrustResourceUrl(
            `/#/documentation/${this.appService.language}/wiki/` +
            this.routing
                .filter(item => item.path.startsWith(this.appService.language))
                .map(item => item.children)
                .flat()
                .find(item => item.path == id).path
            + "?wiki=true"
        );
      }
    })
  }
}

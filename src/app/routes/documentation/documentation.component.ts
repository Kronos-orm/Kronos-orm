import {Component, EventEmitter, OnDestroy, ViewChild} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronosNgDocSidebar/kronos-ng-doc-sidebar.component";
import {AppService} from "../../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {Router} from "@angular/router";
import {NgDocThemeToggleComponent} from "@ng-doc/app";
import {NgDocButtonIconComponent, NgDocIconComponent, NgDocTooltipDirective} from "@ng-doc/ui-kit";
import {DomSanitizer} from "@angular/platform-browser";
import {OverlayPanel} from "primeng/overlaypanel";
import { debounceTime } from 'rxjs/operators';

@Component({
    selector: 'app-documentation',
    standalone: true,
    imports: [
        SharedModule,
        KronosNgDocSidebarComponent,
        NgDocThemeToggleComponent,
        NgDocIconComponent,
        NgDocButtonIconComponent,
        NgDocTooltipDirective
    ],
    templateUrl: './documentation.component.html',
    styleUrl: './documentation.component.scss'
})
export class DocumentationComponent implements OnDestroy {
    constructor(private appService: AppService, private translocoService: TranslocoService, private router: Router, private _sanitizer: DomSanitizer) {
        window.onWikiChange = new EventEmitter<{ id: string, event: MouseEvent }>();
        window.onWikiChange.subscribe(({id, event}) => {
            this.title = id;
            if (this.wikiPanel) {
                this.wikiPanel!!.toggle(event);
            }
        })
    }

    @ViewChild("wikiPanel", {static: false}) wikiPanel: OverlayPanel | undefined;

    get language(): string {
        return this.appService.language;
    }

    async setLang(lang: string) {
        this.appService.language = lang; // update language
        this.translocoService.setActiveLang(lang);
        const newUrl = `/documentation/${lang}/${this.router.url.split("/").slice(3).join("/")}`;
        await this.router.navigate([newUrl]);
    }

    title: string = "";
    url: string = "";

    ngOnDestroy() {
        window.onWikiChange.unsubscribe();
    }
}

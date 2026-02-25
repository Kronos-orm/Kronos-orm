import {Component, ElementRef, EventEmitter, OnDestroy, ViewChild} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronos-ng-doc-sidebar/kronos-ng-doc-sidebar.component";
import {AppService} from "../../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {NgDocThemeToggleComponent} from "@ng-doc/app";
import {NgDocButtonIconComponent, NgDocIconComponent, NgDocTooltipDirective} from "@ng-doc/ui-kit";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";
import {NG_DOC_ROUTING} from "@ng-doc/generated";
import {WikiComponent} from "../../components/wiki.component";
import {FooterComponent} from "../home/components/footer.component";
import {Popover} from "primeng/popover";
import {UIListComponent} from "../../components/ui-list.component";

@Component({
    selector: 'app-documentation',
    standalone: true,
    imports: [
        SharedModule,
        KronosNgDocSidebarComponent,
        NgDocThemeToggleComponent,
        NgDocIconComponent,
        NgDocButtonIconComponent,
        NgDocTooltipDirective,
        WikiComponent,
        FooterComponent,
        RouterLink,
        Popover
    ],
    templateUrl: './documentation.component.html',
    styleUrl: './documentation.component.css'
})
export class DocumentationComponent implements OnDestroy {
    wikiMode = false;

    constructor(
        private appService: AppService,
        private translocoService: TranslocoService,
        private router: Router,
        public elementRef: ElementRef) {
        this.wikiMode = window.frames.length !== parent.frames.length;
    }

    get language(): string {
        return this.appService.language;
    }

    async setLang(lang: string) {
        this.appService.language = lang; // update language
        this.translocoService.setActiveLang(lang);
        const newUrl = `/documentation/${lang}/${this.router.url.split("/").slice(3).join("/")}`;
        await this.router.navigate([newUrl]);
    }

    ngOnDestroy() {
        window.onWikiChange.unsubscribe();
    }
}

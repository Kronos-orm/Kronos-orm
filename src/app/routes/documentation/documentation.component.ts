import {Component} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronosNgDocSidebar/kronos-ng-doc-sidebar.component";
import {AppService} from "../../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {Router} from "@angular/router";
import {NgDocThemeToggleComponent} from "@ng-doc/app";
import {NgDocButtonIconComponent, NgDocIconComponent, NgDocTooltipDirective} from "@ng-doc/ui-kit";
import {DomSanitizer, SafeUrl} from "@angular/platform-browser";

function onBodyAttributePopupDialogChanged() {

}

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
export class DocumentationComponent {
    constructor(private appService: AppService, private translocoService: TranslocoService, private router: Router, private _sanitizer: DomSanitizer) {
        const observer = new MutationObserver(mutations => {
            // There could be one or more mutations so one way to access them is with a loop.
            mutations.forEach(record => {
                // In each iteration an individual mutation can be accessed.
                console.log(record);

                // In this case if the type of mutation is of attribute run the following block.
                // A mutation could have several types.
                if(record.type === 'attributes') {
                    const changedAttrName  = record.attributeName;
                    const newValue = (record.target as HTMLElement).getAttribute(changedAttrName!!);
                    console.log(`Attribute changed! New value for '${changedAttrName}' : ${newValue}`);
                }
            });
        });
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

    get url(): SafeUrl {
        return this._sanitizer.bypassSecurityTrustResourceUrl(document.querySelector("#popup-dialog")?.getAttribute("data-url") ?? "");
    }


}

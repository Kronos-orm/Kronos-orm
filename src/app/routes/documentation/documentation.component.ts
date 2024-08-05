import {Component} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronosNgDocSidebar/kronos-ng-doc-sidebar.component";
import {AppService} from "../../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {Router} from "@angular/router";

@Component({
  selector: 'app-documentation',
  standalone: true,
  imports: [
    SharedModule,
    KronosNgDocSidebarComponent
  ],
  templateUrl: './documentation.component.html',
  styleUrl: './documentation.component.scss'
})
export class DocumentationComponent {
  constructor(private appService: AppService, private translocoService: TranslocoService, private router: Router) {
  }

  setLang(lang: string) {
    this.appService.language = lang; // update language
    this.translocoService.setActiveLang(lang);
    const newUrl = `/documentation/${lang}/${this.router.url.split("/").slice(3).join("/")}`;
    this.router.navigate([newUrl]);
  }
}

import {Component, OnInit} from '@angular/core';
import {SharedModule} from "./shared.module";
import {PrimeNGConfig} from 'primeng/api';
import {AppService} from "./app.service";
import {TranslocoService} from "@jsverse/transloco";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [SharedModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  providers: [AppService]
})
export class AppComponent implements OnInit {
  constructor(private primengConfig: PrimeNGConfig, private translocoService: TranslocoService, private appService: AppService) {
  }

  title = "Kronos"

  ngOnInit() {
    this.primengConfig.ripple = true;
    this.translocoService.setActiveLang(this.appService.language);
  }
}

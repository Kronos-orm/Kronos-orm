import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";
import {AppService} from "../../../app.service";
import {TypewriterComponent} from "./typewriter.component";
import {BannerImgComponent} from "./banner-img.component";

@Component({
  selector: 'banner',
  imports: [
    SharedModule,
    TranslocoPipe,
    TypewriterComponent,
    BannerImgComponent
  ],
  template: `
    <typewriter class="mt-8 hidden xl:block"/>
    <banner-img class="mt-8 block xl:hidden" />
    <h1 class="text-6xl font-bold text-center xl:text-left banner-text">
      {{ "REDEFINE" | transloco }}
      <span class="font-bold text-primary">Kotlin</span>
      <span class="font-bold text-purple-200 ml-3">ORM</span>
    </h1>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-5">
      {{ ("DESCRIPTION1") | transloco }}
    </p>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-5">
      {{ ("DESCRIPTION2") | transloco }}
    </p>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-5">
      {{ ("DESCRIPTION3") | transloco }}
    </p>
    <div class="flex align-items-center gap-3">
      <p-button [routerLink]="['/documentation/' + appService.language +'/getting-started/introduce']" [label]="'GET_START' | transloco" icon="pi pi-arrow-right" severity="info" size="large"/>
      <a href="https://github.com/Kronos-orm/Kronos-orm" target="_blank" 
         class="p-ripple p-element p-button p-component font-bold p-button-lg p-button-warning">
        <i class="pi pi-github mr-3"></i>
        <span>{{ "GIVE_A_STAR" | transloco }}</span>
        <i class="pi pi-star-fill ml-3 text-yellow-500"></i>
      </a>
    </div>
  `,
  standalone: true,
  styles: [`
    a {
      text-decoration: none;
    }

    .banner-text{
      text-shadow: rgba(255, 255, 255, 0.5) 0 5px 12px;
    }
  `]
})
export class BannerComponent {
  constructor(public appService: AppService) {
  }
}

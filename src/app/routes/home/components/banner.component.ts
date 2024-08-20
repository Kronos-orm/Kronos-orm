import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";
import {AppService} from "../../../app.service";

@Component({
  selector: 'banner',
  imports: [
    SharedModule,
    TranslocoPipe
  ],
  template: `
    <div class="banner-img relative">
      <div class="banner-image-overlay-1"></div>
      <canvas style="width: 100%;height: 100%"></canvas>
      <div class="banner-image-overlay-2"></div>
    </div>
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
      <p-button [routerLink]="['/documentation/' + appService.language +'/getting-started/welcome']" [label]="'GET_START' | transloco" icon="pi pi-arrow-right" severity="info" size="large"/>
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
    
    .banner-img {
      background: url('/assets/images/banner.png') no-repeat center center;
      background-size: 100%;
      width: 100%;
      height: 350px;
      mix-blend-mode: exclusion;
      filter: invert(1);
      overflow: hidden;
      animation: scrollY 60s infinite linear;
    }

    @keyframes scrollY {
      0% {
        background-position: 0 0;
      }
      100% {
        background-position: 0 100%;
      }
    }

    .banner-text{
      text-shadow: rgba(255, 255, 255, 0.5) 0 5px 12px;
    }

    .banner-image-overlay-1{
      background-image: linear-gradient(rgba(255, 255, 255, 1), rgba(255, 255, 255, 0));
      width: 100%;
      height: 100px;
      position: absolute;
      top: 0;
      left: 0;
    }

    .banner-image-overlay-2{
      background-image: linear-gradient(rgba(255, 255, 255, 0), rgba(255, 255, 255, 1));
      width: 100%;
      height: 100px;
      position: absolute;
      bottom: 0;
      left: 0;
    }
  `]
})
export class BannerComponent {
  constructor(public appService: AppService) {
  }
}

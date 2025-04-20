import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";
import {AppService} from "../../../app.service";
import {TypewriterComponent} from "./typewriter.component";
import {BannerImgComponent} from "./banner-img.component";
import {RouterLink} from "@angular/router";

@Component({
  selector: 'banner',
  imports: [
    SharedModule,
    TranslocoPipe,
    TypewriterComponent,
    BannerImgComponent,
    RouterLink
  ],
  template: `
    <typewriter class="mt-20 hidden! xl:!block"/>
    <banner-img class="mt-20 !block xl:hidden!"/>
    <h1 class="text-5xl font-bold text-center xl:text-left banner-text">
      <span class="mr-1">{{ "REDEFINE" | transloco }}</span>
      <span class="animated-text">
        <span class="font-bold">Kotlin ORM</span>
      </span>
    </h1>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-8">
      {{ ("DESCRIPTION1") | transloco }}
    </p>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-8">
      {{ ("DESCRIPTION2") | transloco }}
    </p>
    <p class="section-detail xl:text-left text-center px-0 mt-0 mb-8">
      {{ ("DESCRIPTION3") | transloco }}
    </p>
    <div class="flex items-center justify-center gap-4">
      <p-button
          styleClass="pl-8! pr-8!"
          [routerLink]="['/documentation/' + appService.language +'/getting-started/introduce']"
          [label]="'GET_START' | transloco"
          icon="pi pi-bookmark"
          severity="contrast"
          size="large"/>
      <a href="https://github.com/Kronos-orm/Kronos-orm" target="_blank"
         class="p-ripple p-element p-button p-component font-bold p-button-lg p-button-secondary p-button-raised">
        <i class="pi pi-github mr-4"></i>
        <span>{{ "GIVE_A_STAR" | transloco }}</span>
        <i class="pi pi-star-fill ml-4 text-yellow-500"></i>
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

    .animated-text {
      position: relative;
      color: var(--color-white);
      padding: .25rem .5rem;
      border-radius: var(--border-radius);
      display: inline-block;
      width: 17.5rem;
    }

    .animated-text:before {
      border-radius: var(--border-radius);
      animation: color-animation 2s linear infinite;
      background-clip: border-box;
      content: "";
      width: 17.5rem;
      height: 1.5rem;
      position: absolute;
      z-index: 0;
      background-image: linear-gradient(-225deg,var(--color-blue-400) 30%,var(--color-cyan-400) 60%,var(--color-purple-400) 80%);
      filter: blur(24px);
      opacity: .6
    }

    .animated-text>span {
      position: relative;
      z-index: 3;
      background-image: linear-gradient(-225deg,var(--color-blue-400) 30%,var(--color-cyan-400) 60%,var(--color-purple-400) 80%);
      animation: color-animation 2s linear infinite;
      background-size: 200% auto;
      background-clip: text;
      text-fill-color: transparent;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent
    }

    @keyframes color-animation {
      40%,to {
        background-position: -200% center
      }
    }

    @keyframes scroll {
      0% {
        transform: translate(0)
      }

      to {
        transform: translate(calc(-100% - 3rem))
      }
    }
  `]
})
export class BannerComponent {
  constructor(public appService: AppService) {
  }
}

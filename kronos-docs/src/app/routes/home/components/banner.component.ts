import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";
import {AppService} from "../../../app.service";
import {TypewriterComponent} from "./typewriter.component";
import {RouterLink} from "@angular/router";

@Component({
  selector: 'banner',
  imports: [
    SharedModule,
    TranslocoPipe,
    TypewriterComponent,
    RouterLink
  ],
  template: `
    <typewriter class="mt-20"/>
    <h1 class="text-5xl font-bold text-center xl:text-left banner-text">
      <span class="mr-1">{{ "REDEFINE" | transloco }}</span>
      <span class="animated-text">
        <span class="font-bold">Kotlin ORM</span>
      </span>
    </h1>
    <div class="desc-list">
      <div class="desc-item">
        <div class="desc-icon">
          <i class="pi pi-bolt"></i>
        </div>
        <p class="desc-text">{{ "DESCRIPTION1" | transloco }}</p>
      </div>
      <div class="desc-item">
        <div class="desc-icon">
          <i class="pi pi-code"></i>
        </div>
        <p class="desc-text">{{ "DESCRIPTION2" | transloco }}</p>
      </div>
      <div class="desc-item" *ngIf="('DESCRIPTION3' | transloco).trim()">
        <div class="desc-icon">
          <i class="pi pi-database"></i>
        </div>
        <p class="desc-text">{{ "DESCRIPTION3" | transloco }}</p>
      </div>
    </div>
    <div class="flex items-center justify-center xl:justify-start gap-4 mt-2">
      <p-button
          class="pl-8! pr-8!"
          [routerLink]="['/documentation/' + appService.language +'/getting-started/introduce']"
          [label]="'GET_START' | transloco"
          icon="pi pi-bookmark"
          severity="contrast"
          size="large"/>
      <a href="https://github.com/Kronos-orm/Kronos-orm" target="_blank"
         class="p-ripple p-element p-button p-component font-bold p-button-lg p-button-secondary p-button-raised github-btn">
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

    .github-btn {
      border: 1px solid rgba(255, 255, 255, 0.1);
    }

    .banner-text {
      text-shadow: rgba(127, 82, 255, 0.4) 0 5px 30px, rgba(255, 255, 255, 0.3) 0 2px 8px;
      letter-spacing: -0.02em;
    }

    .desc-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin: 1.5rem 0 2rem;
    }

    .desc-item {
      display: flex;
      align-items: flex-start;
      gap: 0.875rem;
      padding: 0.75rem 1rem;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid rgba(255, 255, 255, 0.04);
      transition: border-color 0.2s ease, background 0.2s ease;
    }

    .desc-item:hover {
      border-color: rgba(127, 82, 255, 0.15);
      background: rgba(127, 82, 255, 0.03);
    }

    .desc-icon {
      flex-shrink: 0;
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: linear-gradient(135deg, rgba(127, 82, 255, 0.15), rgba(0, 200, 255, 0.1));
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
    }

    .desc-icon i {
      font-size: 0.85rem;
      color: #C084FC;
    }

    .desc-text {
      margin: 0;
      font-size: 0.95rem;
      line-height: 1.65;
      color: rgba(255, 255, 255, 0.6);
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
      animation: color-animation 3s linear infinite;
      background-clip: border-box;
      content: "";
      width: 17.5rem;
      height: 1.5rem;
      position: absolute;
      z-index: 0;
      background-image: linear-gradient(-225deg, #7F52FF 0%, #00C8FF 50%, #7F52FF 100%);
      filter: blur(28px);
      opacity: .7;
    }

    .animated-text>span {
      position: relative;
      z-index: 3;
      background-image: linear-gradient(-225deg, #7F52FF 0%, #00C8FF 50%, #C084FC 100%);
      animation: color-animation 3s linear infinite;
      background-size: 200% auto;
      background-clip: text;
      text-fill-color: transparent;
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    @keyframes color-animation {
      40%,to {
        background-position: -200% center
      }
    }

    @media (max-width: 1280px) {
      .desc-list {
        align-items: center;
      }

      .desc-item {
        max-width: 560px;
      }
    }
  `]
})
export class BannerComponent {
  constructor(public appService: AppService) {
  }
}

import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";
import {AppService} from "../../../app.service";
import {TypewriterComponent} from "./typewriter.component";

@Component({
  selector: 'banner-img',
  imports: [
    SharedModule
  ],
  template: `
    <div class="banner-img relative mt-8">
      <div class="banner-image-overlay-1"></div>
      <canvas style="width: 100%;height: 100%"></canvas>
      <div class="banner-image-overlay-2"></div>
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
export class BannerImgComponent {
  constructor() {
  }
}

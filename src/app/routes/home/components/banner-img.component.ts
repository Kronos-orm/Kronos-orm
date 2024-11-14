import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";

@Component({
  selector: 'banner-img',
  imports: [
    SharedModule
  ],
  template: `
    <div class="banner-img relative mt-8">
      <canvas style="width: 100%;height: 100%"></canvas>
    </div>
  `,
  standalone: true,
  styles: [`
    a {
      text-decoration: none;
    }
    
    .banner-img {
      background: url('/assets/images/animation/mobile-banner1.gif') no-repeat center center;
      background-size: 100%;
      width: 100%;
      overflow: hidden;
      // 4s后将背景图片切换到banner2.gif，然后永远不会再切换
      animation: banner-img 4s forwards; // 4s后切换到banner2.gif
    }
    
    @keyframes banner-img { // 从1切换到2，且没有过度
      0% {
        background: url('/assets/images/animation/mobile-banner1.gif') no-repeat center center;
        background-size: 100%;
      }
      99% {
        background: url('/assets/images/animation/mobile-banner1.gif') no-repeat center center;
        background-size: 100%;
      }
      100% {
        background: url('/assets/images/animation/mobile-banner2.gif') no-repeat center center;
        background-size: 100%;
      }
    }
  `]
})
export class BannerImgComponent {
  constructor() {
  }
}

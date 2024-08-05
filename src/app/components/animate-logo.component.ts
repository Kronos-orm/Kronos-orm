import {
  Component,
} from '@angular/core';
import {SharedModule} from "../shared.module";

@Component({
  selector: 'animate-logo',
  imports: [
    SharedModule
  ],
  template: `
    <div class="bg">
      <img class="logo" src="/assets/images/logo_circle.png" />
    </div>
  `,
  standalone: true,
  styles: [
    `
      :host {
        text-align: center;
        display: block;
        width: 100%;
      }

      .bg {
        padding: 20px;
        background: linear-gradient(45deg, #832E3D 0%, #000 20%, #7F52FF 40%, #832E3D 60%, #000 80%, #7F52FF 100%);
        background-size: 500% 500%;
        animation: gradient 12s linear infinite;
      }

      @keyframes gradient {
        0% {
          background-position: 100% 0
        }
        100% {
          background-position: 25% 100%
        }
      }

      .logo {
        width: 100px;
        height: 100px;
        animation-name: spin;
        animation-duration: 27.00s;
        animation-iteration-count: infinite;
        animation-timing-function: linear;
        mix-blend-mode: exclusion;
      }

      @keyframes spin {
        0% {
          transform: rotate(0deg);
        }
        100% {
          transform: rotate(360deg);
        }
      }
    `
  ]
})
export class AnimateLogoComponent{}

import {
  Component,
} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {LayoutMenuBarComponent} from "../home/components/layout-menu-bar.component";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  selector: 'exception-404',
  imports: [
    SharedModule,
    LayoutMenuBarComponent,
    TranslocoPipe
  ],
  template: `
    <layout-menu-bar/>
    <div class="flex card flex-column align-items-center justify-content-center gap-5 sm:p-8 h-screen">
      <div class="flex flex-column sm:flex-row align-items-center justify-content-center gap-3 text-primary">
        <span class="font-bold" style="font-size: 144px;"> 4 </span>
        <div class="flex align-items-center justify-content-center bg-primary border-circle w-8rem h-8rem">
          <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" class="logo" draggable="false"
               [style.width.px]="60" alt="logo"/>
        </div>
        <span class="font-bold" style="font-size: 144px;"> 4 </span>
      </div>
      <div
        class="font-bold text-900 text-center text-6xl border-top-1 surface-border pt-5">{{ 'PAGE_NOT_FOUND' | transloco }}
      </div>
      <p-button [label]="'GO_TO_HOME' | transloco" routerLink="/"></p-button>
    </div>
  `,
  standalone: true,
  styles: [
    `
      .logo {
        mix-blend-mode: color-dodge;
        transform: scale(2.5);
      }
    `
  ]
})
export class Exception404Component {
}

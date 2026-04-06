import {
  Component,
} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {LayoutMenuBarComponent} from "../home/components/layout-menu-bar.component";
import {TranslocoPipe} from "@jsverse/transloco";
import {FooterComponent} from "../home/components/footer.component";

@Component({
  selector: 'exception-404',
  imports: [
    SharedModule,
    LayoutMenuBarComponent,
    TranslocoPipe,
    FooterComponent
  ],
  template: `
    <layout-menu-bar/>
    <div class="flex card flex-col items-center justify-center gap-8 sm:p-20 h-screen">
      <div class="flex flex-col sm:flex-row items-center justify-center gap-4 text-primary">
        <span class="font-bold" style="font-size: 144px;"> 4 </span>
        <div class="flex items-center justify-center bg-primary text-primary-contrast rounded-full w-32 h-32">
          <img src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" class="logo" draggable="false"
               [style.width.px]="60" alt="logo"/>
        </div>
        <span class="font-bold" style="font-size: 144px;"> 4 </span>
      </div>
      <div
        class="font-bold text-surface-900 dark:text-surface-0 text-center text-6xl border-t border-surface pt-8">{{ 'PAGE_NOT_FOUND' | transloco }}
      </div>
      <p-button [label]="'GO_TO_HOME' | transloco" routerLink="/"></p-button>
    </div>
    <kronos-footer/>
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

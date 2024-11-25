import {
  Component,
} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  selector: 'feature-cards',
  imports: [
    SharedModule,
    AnimateOnScrollModule,
    TranslocoPipe
  ],
  template: `
    <div pAnimateOnScroll enterClass="zoomin" class="card flex flex-row flex-wrap md:justify-content-between md:align-items-stretch gap-3 animation-duration-1000 animation-ease-in-out">
      <p-card class="flex-1 flex align-items-center justify-content-center min-w-full md:min-w-0" pRipple [header]="'FEATURE_1' | transloco"/>
      <p-card class="flex-1 flex align-items-center justify-content-center min-w-full md:min-w-0" pRipple [header]="'FEATURE_2' | transloco"/>
      <p-card class="flex-1 flex align-items-center justify-content-center min-w-full md:min-w-0" pRipple [header]="'FEATURE_3' | transloco"/>
    </div>
  `,
  standalone: true,
  styles: [`
    ::ng-deep .p-card {
      width: 100%;
      height: 100%;
    }
  `]
})
export class FeatureCardsComponent {
}

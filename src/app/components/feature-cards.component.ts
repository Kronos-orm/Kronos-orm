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
    <div pAnimateOnScroll enterClass="zoomin" class="card flex flex-row md:justify-content-between gap-3 animation-duration-1000 animation-ease-in-out">
      <p-card pRipple [header]="'FEATURE_1' | transloco"/>
      <p-card pRipple [header]="'FEATURE_2' | transloco"/>
      <p-card pRipple [header]="'FEATURE_3' | transloco"/>
    </div>
  `,
  standalone: true,
  styles: []
})
export class FeatureCardsComponent {
}

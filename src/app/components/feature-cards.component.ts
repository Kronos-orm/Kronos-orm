import {
  Component,
} from '@angular/core';
import {SharedModule} from "../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";

@Component({
  selector: 'feature-cards',
  imports: [
    SharedModule,
    AnimateOnScrollModule
  ],
  template: `
    <div pAnimateOnScroll enterClass="zoomin" class="card flex flex-row md:justify-content-between gap-3 animation-duration-1000 animation-ease-in-out">
      <p-card pRipple header="Write Kotlin ORM in Simple and Type-safe Way"/>
      <p-card pRipple header="Less runtime reflect, higher runtime efficiency"/>
      <p-card pRipple header="Multiple database dialect support"/>
    </div>
  `,
  standalone: true,
  styles: []
})
export class FeatureCardsComponent {
}

import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";

@Component({
  selector: 'features',
  imports: [
    SharedModule,
    AnimateOnScrollModule,
  ],
  template: `
    <div pAnimateOnScroll enterClass="fadeinleft" leaveClass="fadeoutleft" class="flex flex-wrap justify-content-center gap-3 overflow-hidden animation-duration-1000 animation-ease-in-out">
      <p-card header="Kotlin multiplatform support" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '525px'}">
        <ng-template pTemplate="header">
          <img class="card-cover" alt="Kotlin multiplatform support" src="/assets/images/features/img-1.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
      <p-card header="Less runtime reflect, more compile-time task" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '525px'}">
        <ng-template pTemplate="header">
          <img class="card-cover" alt="Less runtime reflect" src="/assets/images/features/img-2.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
      <p-card header="Smarter framework, less code, simple solutions" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '525px'}">
        <ng-template pTemplate="header">
          <img class="card-cover" alt="Smarter framework" src="/assets/images/features/img-3.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
      <p-card header="Easy to integrate with third-party frameworks" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '525px'}">
        <ng-template pTemplate="header">
          <img class="card-cover" alt="Easy plugin integration" src="/assets/images/features/img-4.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
    </div>

  `,
  standalone: true,
  styles: [`
    .card-cover {
      height: 20rem;
      object-fit: cover;
    }
  `],
})
export class FeaturesComponent {
}

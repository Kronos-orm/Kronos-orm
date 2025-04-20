import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  selector: 'features',
  imports: [
    SharedModule,
    AnimateOnScrollModule,
    TranslocoPipe,
  ],
  template: `
    <div pAnimateOnScroll enterClass="animate-fadeinleft" leaveClass="animate-fadeoutleft" class="flex flex-wrap justify-center gap-4 overflow-hidden animate-duration-1000 animate-ease-in-out">
      <p-card [header]="'FEATURE_BLOG_TITLE_1' | transloco" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '425px'}">
        <ng-template pTemplate="header">
          <img class="w-full" alt="Kotlin multiplatform support" src="/assets/images/features/img-1.png" />
        </ng-template>
        <p>
          {{"FEATURE_BLOG_CONTENT_1" | transloco}}
        </p>
        <a class="p-button p-button-contrast" href="/#/blog?blog=kotlin-multiplatform-support">{{ "READ_MORE" | transloco }}</a>
      </p-card>
      <p-card header="Less runtime reflect, more compile-time task" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '425px'}">
        <ng-template pTemplate="header">
          <img class="w-full" alt="Less runtime reflect" src="/assets/images/features/img-2.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
      <p-card header="Smarter framework, less code, simple solutions" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '425px'}">
        <ng-template pTemplate="header">
          <img class="w-full" alt="Smarter framework" src="/assets/images/features/img-3.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
      <p-card header="Easy to integrate with third-party frameworks" subheader="Kronos" [style]="{ minWidth: 'calc(100vw / 3)', maxWidth: '425px'}">
        <ng-template pTemplate="header">
          <img class="w-full" alt="Easy plugin integration" src="/assets/images/features/img-4.png" />
        </ng-template>
        <p>
          Some content is waiting to be written here...
        </p>
      </p-card>
    </div>

  `,
  standalone: true,
})
export class FeaturesComponent {
}

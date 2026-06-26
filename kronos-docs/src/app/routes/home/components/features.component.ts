import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  selector: 'features',
  imports: [
    SharedModule,
    TranslocoPipe,
  ],
  template: `
    <div class="flex flex-wrap justify-center gap-6 overflow-hidden">
      <div class="feature-card" *ngFor="let card of cards">
        <div class="feature-card-inner">
          <div class="feature-card-image">
            <img [alt]="card.alt" [src]="card.image" />
          </div>
          <div class="feature-card-body">
            <h3 class="feature-card-title">{{ card.title | transloco }}</h3>
            <span class="feature-card-subtitle">Kronos</span>
            <p class="feature-card-text">{{ card.content | transloco }}</p>
            <a *ngIf="card.href" class="feature-card-link" [href]="card.href">
              {{ "READ_MORE" | transloco }}
              <i class="pi pi-arrow-right ml-2"></i>
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
  standalone: true,
  styles: [`
    .feature-card {
      flex: 1 1 300px;
      max-width: 420px;
      min-width: 280px;
    }

    .feature-card-inner {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.06);
      border-radius: 16px;
      overflow: hidden;
      transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .feature-card-inner:hover {
      transform: translateY(-4px);
      border-color: rgba(127, 82, 255, 0.3);
      box-shadow: 0 8px 32px rgba(127, 82, 255, 0.1), 0 0 0 1px rgba(127, 82, 255, 0.1);
    }

    .feature-card-image {
      overflow: hidden;
    }

    .feature-card-image img {
      width: 100%;
      display: block;
      transition: transform 0.4s ease;
    }

    .feature-card-inner:hover .feature-card-image img {
      transform: scale(1.05);
    }

    .feature-card-body {
      padding: 1.25rem 1.5rem 1.5rem;
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .feature-card-title {
      font-size: 1.15rem;
      font-weight: 600;
      color: #fff;
      margin: 0 0 0.25rem;
    }

    .feature-card-subtitle {
      font-size: 0.8rem;
      color: rgba(255, 255, 255, 0.35);
      font-weight: 500;
      margin-bottom: 0.75rem;
    }

    .feature-card-text {
      font-size: 0.9rem;
      color: rgba(255, 255, 255, 0.55);
      line-height: 1.6;
      flex: 1;
    }

    .feature-card-link {
      display: inline-flex;
      align-items: center;
      margin-top: 1rem;
      padding: 0.5rem 1.25rem;
      border-radius: 8px;
      background: rgba(127, 82, 255, 0.1);
      border: 1px solid rgba(127, 82, 255, 0.2);
      color: #C084FC;
      font-weight: 500;
      font-size: 0.85rem;
      text-decoration: none;
      transition: background 0.2s ease, border-color 0.2s ease;
      width: fit-content;
    }

    .feature-card-link:hover {
      background: rgba(127, 82, 255, 0.2);
      border-color: rgba(127, 82, 255, 0.4);
    }
  `]
})
export class FeaturesComponent {
  cards = [
    {
      title: 'FEATURE_BLOG_TITLE_1',
      content: 'FEATURE_BLOG_CONTENT_1',
      image: '/assets/images/features/img-1.png',
      alt: 'Kotlin multiplatform support',
      href: '/#/blog?blog=kotlin-multiplatform-support'
    },
    {
      title: 'FEATURE_BLOG_TITLE_2',
      content: 'FEATURE_BLOG_CONTENT_2',
      image: '/assets/images/features/img-3.png',
      alt: 'Smarter framework',
      href: '/#/blog?blog=code-generation-support'
    },
    {
      title: 'Less runtime reflect, more compile-time task',
      content: 'Some content is waiting to be written here...',
      image: '/assets/images/features/img-2.png',
      alt: 'Less runtime reflect',
      href: null
    },
    {
      title: 'Easy to integrate with third-party frameworks',
      content: 'Some content is waiting to be written here...',
      image: '/assets/images/features/img-4.png',
      alt: 'Easy plugin integration',
      href: null
    }
  ];
}

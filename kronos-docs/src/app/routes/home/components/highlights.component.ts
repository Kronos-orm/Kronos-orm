import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe} from "@jsverse/transloco";

@Component({
  selector: 'highlights',
  imports: [SharedModule, TranslocoPipe],
  template: `
    <div class="highlights-section">
      <div class="highlights-header">
        <h2 class="highlights-title">{{ 'HIGHLIGHT_TITLE' | transloco }}</h2>
        <p class="highlights-subtitle">{{ 'HIGHLIGHT_SUBTITLE' | transloco }}</p>
      </div>
      <div class="highlights-grid">
        <div class="hl-card" *ngFor="let item of items">
          <div class="hl-icon">{{ item.icon }}</div>
          <h3 class="hl-title">{{ item.title | transloco }}</h3>
          <p class="hl-desc">{{ item.desc | transloco }}</p>
        </div>
      </div>
    </div>
  `,
  standalone: true,
  styles: [`
    .highlights-section {
      padding: 2rem 0;
    }

    .highlights-header {
      text-align: center;
      margin-bottom: 3rem;
    }

    .highlights-title {
      font-size: 2rem;
      font-weight: 700;
      color: #fff;
      margin: 0 0 0.75rem;
      background: linear-gradient(135deg, #fff 0%, #C084FC 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .highlights-subtitle {
      font-size: 1rem;
      color: rgba(255, 255, 255, 0.45);
      margin: 0;
    }
    .highlights-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1.5rem;
    }

    .hl-card {
      padding: 2rem 1.75rem;
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid rgba(255, 255, 255, 0.06);
      transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
    }

    .hl-card:hover {
      transform: translateY(-4px);
      border-color: rgba(127, 82, 255, 0.25);
      box-shadow: 0 8px 30px rgba(127, 82, 255, 0.08);
    }

    .hl-icon {
      font-size: 2rem;
      margin-bottom: 1rem;
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px;
      background: rgba(127, 82, 255, 0.08);
    }

    .hl-title {
      font-size: 1.1rem;
      font-weight: 600;
      color: #fff;
      margin: 0 0 0.5rem;
    }

    .hl-desc {
      font-size: 0.875rem;
      color: rgba(255, 255, 255, 0.5);
      line-height: 1.65;
      margin: 0;
    }

    @media (max-width: 1024px) {
      .highlights-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 640px) {
      .highlights-grid {
        grid-template-columns: 1fr;
      }

      .highlights-title {
        font-size: 1.5rem;
      }
    }
  `]
})
export class HighlightsComponent {
  items = [
    { icon: '🚀', title: 'HL_ZERO_REFLECT_TITLE', desc: 'HL_ZERO_REFLECT_DESC' },
    { icon: '🔗', title: 'HL_CASCADE_TITLE', desc: 'HL_CASCADE_DESC' },
    { icon: '🔄', title: 'HL_CODEFIRST_TITLE', desc: 'HL_CODEFIRST_DESC' },
    { icon: '🔒', title: 'HL_TRANSACTION_TITLE', desc: 'HL_TRANSACTION_DESC' },
    { icon: '🏢', title: 'HL_MULTITENANT_TITLE', desc: 'HL_MULTITENANT_DESC' },
    { icon: '🧩', title: 'HL_PLUGIN_TITLE', desc: 'HL_PLUGIN_DESC' },
  ];
}

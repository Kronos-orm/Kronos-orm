import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AppService} from "../../../app.service";

@Component({
  selector: 'kronos-footer',
  imports: [
    SharedModule
  ],
  template: `
      <footer class="footer-container">
          <div class="footer-inner">
              <div class="footer-links">
                  <a href="https://cloudflare.com" target="_blank">
                      <img src="/assets/icons/cloudflare.svg" alt="cloudflare" width="90"/>
                      <span>Cloudflare</span>
                  </a>
                  <a href="https://primeng.org" target="_blank">
                      <img src="/assets/icons/primeng.svg" alt="primeng" width="90"/>
                      <span>PrimeNG</span>
                  </a>
              </div>
              <div class="footer-meta">
                  <span class="footer-copyright">Kronos-ORM &copy; 2026</span>
                  <div class="footer-badges">
                      <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache 2.0</a>
                      <a href="https://github.com/kronos-orm/kronos-orm/stargazers">
                          <img src="https://img.shields.io/github/stars/kronos-orm/kronos-orm.svg?style=social" alt="Stars">
                      </a>
                  </div>
              </div>
          </div>
      </footer>
  `,
  standalone: true,
  styles: [
      `
        .footer-container {
          background: #060609;
          border-top: 1px solid rgba(255, 255, 255, 0.05);
          padding: 2.5rem 2rem;
        }

        .footer-inner {
          max-width: 1200px;
          margin: 0 auto;
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 2rem;
          flex-wrap: wrap;
        }

        .footer-links {
          display: flex;
          gap: 2.5rem;
          align-items: center;
        }

        .footer-links a {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 6px;
          color: rgba(255, 255, 255, 0.5);
          text-decoration: none;
          font-weight: 500;
          font-size: 0.85rem;
          transition: color 0.2s ease;
        }

        .footer-links a:hover {
          color: #C084FC;
        }

        .footer-links img {
          opacity: 0.6;
          transition: opacity 0.2s ease;
        }

        .footer-links a:hover img {
          opacity: 1;
        }

        .footer-meta {
          display: flex;
          flex-direction: column;
          align-items: flex-end;
          gap: 0.75rem;
        }

        .footer-copyright {
          color: rgba(255, 255, 255, 0.4);
          font-size: 0.85rem;
        }

        .footer-badges {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .footer-badges a {
          color: rgba(255, 255, 255, 0.4);
          text-decoration: none;
          font-size: 0.8rem;
          transition: color 0.2s ease;
        }

        .footer-badges a:hover {
          color: #C084FC;
        }

        @media (max-width: 640px) {
          .footer-inner {
            flex-direction: column;
            align-items: center;
            text-align: center;
          }

          .footer-meta {
            align-items: center;
          }
        }
      `
  ]
})
export class FooterComponent {
  constructor(public appService: AppService) {
  }
}

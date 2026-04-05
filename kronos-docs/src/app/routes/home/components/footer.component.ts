import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AppService} from "../../../app.service";

@Component({
  selector: 'kronos-footer',
  imports: [
    SharedModule
  ],
  template: `
      <footer class="flex flex-row justify-center items-center gap-4 bg-dark text-light p-8">
          <div class="flex flex-col flex-wrap justify-center gap-6">
              <div class="flex flex-wrap justify-center gap-6">
                  <a href="https://cloudflare.com" target="_blank">
                      <img src="/assets/icons/cloudflare.svg" alt="cloudflare" width="100"/>
                      <div>Cloudflare</div>
                  </a>
              </div>
              <div class="mt-4">
                  <a href="https://primeng.org" target="_blank">
                      <img src="/assets/icons/primeng.svg" alt="angular" width="100"/>
                      <div>PrimeNG</div>
                  </a>
              </div>
          </div>
          <div class="mt-4 flex flex-col">
              <div class="flex">
                  <span>Kronos-ORM © 2026</span>
              </div>
              <div class="flex mt-6">
                  <a href="https://www.apache.org/licenses/LICENSE-2.0">
                      Apache 2.0
                  </a>
                  <a class="ml-2" href="https://github.com/kronos-orm/kronos-orm/stargazers">
                      <img src="https://img.shields.io/github/stars/kronos-orm/kronos-orm.svg?style=social" alt="Stars">
                  </a>
              </div>
          </div>
      </footer>
  `,
  standalone: true,
  styles: [
      `
        a {
          color: var(--ng-doc-text);
          text-decoration: none;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 5px;
          font-weight: 600;
        }

        a:hover {
          color: var(--ng-doc-primary);
          text-decoration: underline;
        }
      `
  ]
})
export class FooterComponent {
  constructor(public appService: AppService) {
  }
}

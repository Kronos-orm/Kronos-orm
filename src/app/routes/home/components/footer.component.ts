import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AppService} from "../../../app.service";

@Component({
  selector: 'kronos-footer',
  imports: [
    SharedModule
  ],
  template: `
      <footer class="flex flex-row justify-content-center align-items-center gap-3 bg-dark text-light p-5">
          <div class="flex flex-column flex-wrap justify-content-center gap-4">
              <div class="flex flex-wrap justify-content-center gap-4">
                  <a href="https://cloudflare.com" target="_blank">
                      <img src="/assets/icons/cloudflare.svg" alt="cloudflare" width="100"/>
                      <div>Cloudflare</div>
                  </a>
              </div>
              <div class="mt-3">
                  <a href="https://primeng.org" target="_blank">
                      <img src="/assets/icons/primeng.svg" alt="angular" width="100"/>
                      <div>PrimeNG</div>
                  </a>
              </div>
          </div>
          <div class="mt-3 flex">
              <span>Kronos-ORM</span>
              <span class="mx-2">© 2022 - 2024</span>
              <a href="https://www.apache.org/licenses/LICENSE-2.0" class="text-light">
                  Apache 2.0
              </a>
              <a class="mx-2" href="https://github.com/kronos-orm/kronos-orm/stargazers">
                  <img src="https://img.shields.io/github/stars/kronos-orm/kronos-orm.svg?style=social" alt="Stars">
              </a>
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

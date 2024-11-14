import { Component } from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {AppService} from "../../../app.service";

@Component({
  selector: 'kronos-footer',
  imports: [
    SharedModule
  ],
  template: `
    <footer class="flex flex-col justify-content-center align-items-center gap-3 bg-dark text-light p-5">
      KRONOS © 2024 <img src="/assets/icons/cloudflare.svg" alt="cloudflare" width="80"/>
    </footer>
  `,
  standalone: true,
  styles: []
})
export class FooterComponent {
  constructor(public appService: AppService) {
  }
}

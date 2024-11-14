import {Component} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {LayoutMenuBarComponent} from "./components/layout-menu-bar.component";
import {TerminalService} from "primeng/terminal";
import {OnlineEditingComponent} from "./components/online-coding/online-editing.component";
import {BannerComponent} from "./components/banner.component";
import {FeaturesComponent} from "./components/features.component";
import {BannerMarqueeComponent} from "./components/banner-marquee.component";
import {InstallPackageComponent} from "./components/install-package.component";
import {FooterComponent} from "./components/footer.component";

@Component({
  selector: 'app-home',
  standalone: true,
    imports: [
        SharedModule,
        LayoutMenuBarComponent,
        OnlineEditingComponent,
        BannerComponent,
        FeaturesComponent,
        BannerMarqueeComponent,
        InstallPackageComponent,
        FooterComponent
    ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  providers: [TerminalService]
})
export class HomeComponent {
}

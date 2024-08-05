import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {NgDocNavbarComponent, NgDocRootComponent, NgDocSidebarComponent} from "@ng-doc/app";
import {RouterOutlet} from "@angular/router";
import {ButtonModule} from "primeng/button";
import {RippleModule} from 'primeng/ripple';
import {MegaMenuModule} from "primeng/megamenu";
import {DropdownModule} from "primeng/dropdown";
import {FormsModule} from "@angular/forms";
import {OverlayPanelModule} from "primeng/overlaypanel";
import {TableModule} from "primeng/table";
import {MenuModule} from "primeng/menu";
import {TabViewModule} from "primeng/tabview";
import {SkeletonModule} from "primeng/skeleton";
import {TooltipModule} from "primeng/tooltip";
import {TerminalModule} from "primeng/terminal";
import {InputTextModule} from "primeng/inputtext";
import {CardModule} from "primeng/card";

const primengModules = [
  ButtonModule,
  RippleModule,
  MegaMenuModule,
  DropdownModule,
  OverlayPanelModule,
  TableModule,
  TabViewModule,
  MenuModule,
  SkeletonModule,
  TooltipModule,
  TerminalModule,
  InputTextModule,
  CardModule
];

const ngDocModules = [
  NgDocRootComponent,
  NgDocNavbarComponent,
  NgDocSidebarComponent
];

@NgModule({
  imports: [
    CommonModule,
    RouterOutlet,
    FormsModule,
    ...ngDocModules,
    ...primengModules
  ],
  exports: [
    CommonModule,
    RouterOutlet,
    FormsModule,
    ...ngDocModules,
    ...primengModules
  ],
})
export class SharedModule {
}

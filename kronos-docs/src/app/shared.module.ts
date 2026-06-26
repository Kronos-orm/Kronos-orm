import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {NgDocNavbarComponent, NgDocRootComponent, NgDocSidebarComponent} from "@ng-doc/app";
import {RouterOutlet} from "@angular/router";
import {ButtonModule} from "primeng/button";
import {RippleModule} from 'primeng/ripple';
import {MegaMenuModule} from "primeng/megamenu";
import {SelectModule} from "primeng/select";
import {FormsModule} from "@angular/forms";
import {PopoverModule} from "primeng/popover";
import {TableModule} from "primeng/table";
import {MenuModule} from "primeng/menu";
import {SkeletonModule} from "primeng/skeleton";
import {TooltipModule} from "primeng/tooltip";
import {TerminalModule} from "primeng/terminal";
import {InputTextModule} from "primeng/inputtext";
import {CardModule} from "primeng/card";
import {ToastModule} from "primeng/toast";

const primengModules = [
  ButtonModule,
  RippleModule,
  MegaMenuModule,
  SelectModule,
  PopoverModule,
  TableModule,
  MenuModule,
  SkeletonModule,
  TooltipModule,
  TerminalModule,
  InputTextModule,
  CardModule,
  ToastModule
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

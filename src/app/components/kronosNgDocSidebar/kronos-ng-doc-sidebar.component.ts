import {Component, Inject,} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {NG_DOC_CONTEXT} from "@ng-doc/app/tokens";
import {NgDocContext, NgDocNavigation} from "@ng-doc/app";
import {PanelMenuModule} from "primeng/panelmenu";
import {AppService} from "../../app.service";
import {MenuItem} from "primeng/api";
import {StyleClassModule} from "primeng/styleclass";
import {NavigationEnd, Router} from "@angular/router";
import {filter} from "rxjs";

const docSortFn = (a: NgDocNavigation, b: NgDocNavigation) => (a.order ?? 0) - (b.order ?? 0);

@Component({
  selector: 'kronos-ng-doc-sidebar',
  imports: [
    SharedModule,
    PanelMenuModule,
    StyleClassModule
  ],
  template: `
    <p-menu [model]="items" styleClass="w-full md:w-20rem bg-black p-2"/>
  `,
  styleUrl: './kronos-ng-doc-sidebar.component.scss',
  standalone: true,
})
export class KronosNgDocSidebarComponent {
  constructor(@Inject(NG_DOC_CONTEXT) private context: NgDocContext, private _app: AppService, private router: Router) {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateMenuState();
      });
  }

  items: MenuItem[] = [];

  updateMenuState() {
    this.items = this.context.navigation.sort(docSortFn).map((item, index) => {
      return {
        label: item.title,
        items: item.children?.sort(docSortFn).map(child => {
          return {
            label: child.title,
            routerLink: [child.route],
            routerLinkActiveOptions: {exact: true},
            icon: "pi pi-circle-on",
          }
        }),
        routerLinkActiveOptions: {exact: true},
        visible: item.route.startsWith(`/documentation/${this._app.language}/`) && item.title != "Wiki",
      }
    });

    const matchesRouterUrl = (item: MenuItem)=> {
      if (item.routerLink) {
        if (item.routerLinkActiveOptions?.exact === true) {
          return item.routerLink[0] === this.router.url;
        } else {
          return this.router.url.startsWith(item.routerLink[0]);
        }
      }
      return false;
    }


    const expand = (item: MenuItem) => {
      if (matchesRouterUrl(item)) {
        return true;
      } else if (item.items) {
        for (const i of item.items) {
          if (expand(i)) {
            item.expanded = true;
            return true;
          }
        }
      }

      return false;
    }

    this.items.forEach(expand);
    setTimeout(() => {
      const offsetTop = (document.querySelector(".p-menuitem-link-active") as HTMLElement)?.offsetTop;
      if (offsetTop > window.innerHeight) {
        document.querySelector(".p-menu")?.scrollTo({
          behavior: 'smooth',
          top: offsetTop
        })
      }
    }, 50);
  }
}

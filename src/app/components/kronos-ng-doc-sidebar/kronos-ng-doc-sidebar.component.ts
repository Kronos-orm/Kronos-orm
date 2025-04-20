import {Component, Inject,} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {NG_DOC_CONTEXT} from "@ng-doc/app/tokens";
import {NgDocContext, NgDocNavigation} from "@ng-doc/app";
import {PanelMenuModule} from "primeng/panelmenu";
import {AppService} from "../../app.service";
import {MenuItem} from "primeng/api";
import {StyleClassModule} from "primeng/styleclass";
import {NavigationEnd, Router, RouterLink, RouterLinkActive} from "@angular/router";
import {filter} from "rxjs";
import {TagModule} from "primeng/tag";
import {NgDocColor, NgDocTagComponent} from "@ng-doc/ui-kit";

const docSortFn = (a: NgDocNavigation, b: NgDocNavigation) => (a.order ?? 0) - (b.order ?? 0);

@Component({
    selector: 'kronos-ng-doc-sidebar',
    imports: [
        SharedModule,
        PanelMenuModule,
        StyleClassModule,
        NgDocTagComponent,
        RouterLink,
        RouterLinkActive
    ],
    template: `
        <p-menu [model]="items" styleClass="w-full md:w-80 bg-black p-2">
            <ng-template pTemplate="item" let-item>
                <div class="p-menuitem-content" data-pc-section="content">
                    <a [routerLink]="item.routerLink" routerLinkActive="p-menuitem-link-active" pRipple
                       class="p-ripple p-element p-menuitem-link">
                        <i class="p-menuitem-icon" [class]="item.icon"></i>
                        <span class="p-menuitem-text mr-2">{{ item.label }}</span>
                        @for (status of item.statuses; track status) {
                            <ng-doc-tag size="small" [color]="status.type || 'success'" mod="light">{{
                                    status.text
                                }}
                            </ng-doc-tag>
                        }
                    </a>
                </div>
            </ng-template>
        </p-menu>
    `,
    styleUrl: './kronos-ng-doc-sidebar.component.css',
    standalone: true,
})
export class KronosNgDocSidebarComponent {
    constructor(@Inject(NG_DOC_CONTEXT) private context: NgDocContext, private _app: AppService, private router: Router) {
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => this.updateMenuState());
    }

    items: MenuItem[] = [];

    updateMenuState() {
        this.items = this.context.navigation.sort(docSortFn).map((item, index) => {
            return {
                label: item.title,
                items: item.children?.sort(docSortFn).map(child => {
                    return {
                        label: child.title,
                        metadata: child.metadata,
                        statuses: (child.metadata.tags['status'] ?? []).map((status) => {
                            const [type, text] = status.split(/\s+(.+)/);
                            return {type: type.replace(/^:/, '') as NgDocColor, text};
                        }),
                        routerLink: [child.route],
                        routerLinkActiveOptions: {exact: true},
                        icon: "pi pi-circle-on",
                    }
                }),
                routerLinkActiveOptions: {exact: true},
                visible: item.route.startsWith(`/documentation/${this._app.language}/`),
            }
        });

        const matchesRouterUrl = (item: MenuItem) => {
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

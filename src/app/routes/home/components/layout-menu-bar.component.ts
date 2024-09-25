import {Component} from '@angular/core';
import {MegaMenuItem} from "primeng/api";
import {AppService} from "../../../app.service";
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe, TranslocoService} from "@jsverse/transloco";

@Component({
    selector: 'layout-menu-bar',
    imports: [
        SharedModule,
        TranslocoPipe
    ],
    template: `
        <p-megaMenu [model]="items" [styleClass]="'border-none menu-bar p-0 pl-4'">
            <ng-template pTemplate="start">
                <img [routerLink]="['/']" src="https://cdn.leinbo.com/assets/images/kronos/logo_dark.png" class="logo"
                     draggable="false"
                     [style.width.px]="60" alt="logo"/>
                <span [routerLink]="['/']" class="logo">Kronos ORM</span>
            </ng-template>
            <ng-template pTemplate="item" let-item>
                <a *ngIf="item.root" pRipple [routerLink]="item.routerLink" routerLinkActive="p-menuitem-active"
                   class="flex align-items-center p-menuitem-link cursor-pointer px-3 py-2 overflow-hidden relative font-semibold text-lg">
                    <i [ngClass]="item.icon"></i>
                    <span class="mx-2">{{ item.label | transloco }}</span>
                </a>
                <a *ngIf="!item.root && !item.image" [routerLink]="item.routerLink"
                   class="flex align-items-center p-3 cursor-pointer mb-2 gap-2">
                <span
                        class="inline-flex align-items-center justify-content-center border-circle bg-primary w-3rem h-3rem">
                    <i [ngClass]="item.icon + ' text-lg'"></i>
                </span>
                    <span class="inline-flex flex-column gap-1" style="width: calc(100% - 3.5rem)">
                    <span class="font-medium text-lg text-900">{{ item.label | transloco }}</span>
                    <span class="white-space-nowrap text-overflow-ellipsis overflow-hidden text-gray-200">{{ item.subtext | transloco }}</span>
                </span>
                </a>
                <div [routerLink]="item.routerLink" *ngIf="item.image"
                     class="flex flex-column align-items-start gap-3 p-2">
                    <img [src]="item.image" alt="megamenu-demo" class="w-full"/>
                    <span class="text-900 text-gray-200">{{ item.subtext | transloco }}</span>
                    <p-button [label]="item.label | transloco" [outlined]="true"></p-button>
                </div>
            </ng-template>
            <ng-template pTemplate="end">
                <p-button
                        [text]="true"
                        class="mr-4"
                        (click)="op.toggle($event)"
                        icon="pi pi-language"/>
                <p-overlayPanel #op styleClass="m-0 p-0">
                    <ul class="list-none m-0 p-0 w-12rem">
                        @for (item of [{lang: 'zh-CN', label: '简体中文'}, {
                            lang: 'en',
                            label: 'English'
                        }]; track $index) {
                            <a class="block p-2 border-round hover:surface-hover w-full cursor-pointer flex"
                               (click)="setLang(item.lang)">
                                <span class="font-bold text-900 flex-1">{{ item.label }}</span>
                                <span class="ml-2 text-700 flex-1 text-right">
            <span class="pi pi-globe"></span>
          </span>
                            </a>
                        }
                    </ul>
                </p-overlayPanel>
                <a href="https://github.com/Kronos-orm/Kronos-orm"
                   target="_blank"
                   class="mr-4 p-button p-button-text">
                    <i class="pi pi-github"></i>
                </a>
            </ng-template>
        </p-megaMenu>
    `,
    standalone: true,
    styles: [
        `
          :host ::ng-deep {
            .p-megamenu-start {
              margin-right: 24px;
            }

            .menu-bar {
              display: flex;
              background: linear-gradient(45deg, #832E3D 0%, #000 20%, #7F52FF 40%, #832E3D 60%, #000 80%, #7F52FF 100%);
              background-size: 500% 500%;
              animation: gradient 12s linear infinite;
            }

            @keyframes gradient {
              0% {
                background-position: 100% 0
              }
              100% {
                background-position: 25% 100%
              }
            }

            .logo {
              cursor: pointer;
              mix-blend-mode: exclusion;
              transform-origin: center;
              vertical-align: middle;
              font-family: "Poppins", sans-serif;
              font-weight: bolder;

              &:hover {
                filter: grayscale(100%);
                opacity: 0.8;
                transition: .1s ease-in-out;
              }
            }

            .p-menuitem-active {
              color: rgba(255, 255, 255, 0.87);
              background: rgba(255, 255, 255, 0.2);
            }
          }

          a {
            text-decoration: none;
          }
        `
    ]
})
export class LayoutMenuBarComponent {
    items: MegaMenuItem[] | undefined;

    ngOnInit() {
        this.items = [
            {
                label: "HOME",
                root: true,
                icon: 'pi pi-home',
                routerLink: '/home'
            },
            {
                label: 'DOCUMENTATION',
                icon: 'pi pi-book',
                root: true,
                routerLink: `/documentation/${this.appService.language}/getting-started/introduce`,
                items: [
                    [
                        {
                            items: [
                                {
                                    label: 'QUICK_START',
                                    icon: 'pi pi-play',
                                    subtext: 'SUBTEXT_OF_QUICK_START',
                                    routerLink: `/documentation/${this.appService.language}/getting-started/quick-start`
                                },
                                {
                                    label: 'CLASS_DEFINITION',
                                    icon: 'pi pi-list',
                                    subtext: 'SUBTEXT_OF_CLASS_DEFINITION',
                                    routerLink: `/documentation/${this.appService.language}/class-definition/table-class-definition`
                                },
                                {
                                    label: 'CONNECT_TO_DB',
                                    icon: 'pi pi-download',
                                    subtext: 'SUBTEXT_OF_CONNECT_TO_DB',
                                    routerLink: `/documentation/${this.appService.language}/database/connect-to-db`
                                },
                            ]
                        }
                    ],
                    [
                        {
                            items: [
                                {
                                    label: 'DATABASE_OPERATION',
                                    icon: 'pi pi-user',
                                    subtext: 'SUBTEXT_OF_DATABASE_OPERATION',
                                    routerLink: `/documentation/${this.appService.language}/database/database-operation`
                                },
                                {
                                    label: 'SQL_EXECUTE',
                                    icon: 'pi pi-info',
                                    subtext: 'SUBTEXT_OF_SQL_EXECUTE',
                                    routerLink: `/documentation/${this.appService.language}/database/named-arguments-base-sql`
                                },
                                {
                                    label: 'CRITERIA_DEFINITION',
                                    icon: 'pi pi-search',
                                    subtext: 'SUBTEXT_OF_CRITERIA_DEFINITION',
                                    routerLink: `/documentation/${this.appService.language}/database/where-having-on-clause`
                                },
                            ]
                        }
                    ],
                    [
                        {
                            items: [
                                {
                                    label: 'ADVANCED',
                                    icon: 'pi pi-star',
                                    subtext: 'SUBTEXT_OF_ADVANCED',
                                    routerLink: `/documentation/${this.appService.language}/advanced/some-locks`
                                },
                                {
                                    label: 'PLUGIN',
                                    icon: 'pi pi-globe',
                                    subtext: 'SUBTEXT_OF_PLUGIN',
                                    routerLink: `/documentation/${this.appService.language}/plugin/datasource-wrapper-and-third-part-framework`
                                },
                                {
                                    label: 'CHANGELOG',
                                    icon: 'pi pi-clock',
                                    subtext: 'SUBTEXT_OF_CHANGELOG',
                                    routerLink: `/documentation/${this.appService.language}/getting-started/changelog`
                                }
                            ]
                        }
                    ],
                    [
                        {
                            items: [{
                                image: 'https://cdn.leinbo.com/assets/images/kronos/code-cover.jpg',
                                label: 'GET_START',
                                subtext: 'SUBTEXT_OF_QUICK_START',
                                routerLink: [`/documentation/${this.appService.language}/getting-started/quick-start`]
                            }]
                        }
                    ]
                ]
            },
            {
                label: 'RESOURCES',
                icon: 'pi pi-palette',
                root: true,
                items: [
                    [{
                        items: [{
                            image: 'https://cdn.leinbo.com/assets/images/kronos/code-cover.jpg',
                            label: 'Coming soon',
                            subtext: '代码生成器',
                        }]
                    }]
                ]
            },
            {
                label: 'DISCUSSION',
                icon: 'pi pi-comments',
                root: true,
                routerLink: 'https://github.com/Kronos-orm/Kronos-orm/discussions'
            }
        ];
    }

    constructor(private appService: AppService, private translocoService: TranslocoService) {
    }

    setLang(lang: string) {
        this.appService.language = lang; // update language
        this.translocoService.setActiveLang(lang);
    }
}

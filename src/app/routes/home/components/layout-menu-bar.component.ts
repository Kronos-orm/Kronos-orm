import {Component, HostListener, OnInit} from '@angular/core';
import {MegaMenuItem, MessageService} from "primeng/api";
import {AppService} from "../../../app.service";
import {SharedModule} from "../../../shared.module";
import {TranslocoPipe, TranslocoService} from "@jsverse/transloco";
import {ListItem, UIListComponent} from "../../../components/ui-list.component";
import {AnimateOnScrollModule} from "primeng/animateonscroll";
import {RouterLink, RouterLinkActive} from "@angular/router";
import {Popover} from "primeng/popover";

@Component({
    selector: 'layout-menu-bar',
    imports: [
        SharedModule,
        TranslocoPipe,
        UIListComponent,
        AnimateOnScrollModule,
        RouterLink,
        Popover,
        RouterLinkActive
    ],
    template: `
        <div class="bar-container w-full"
             [class]="fixed ? ['fixed', 'top-0', 'fadeinup', 'opacity-90', 'shadow-8', 'fadeinup'] : ['fadeoutup']">
            <p-toast/>
            <p-megaMenu class="hidden! md:block!" [model]="items" [styleClass]="'border-none menu-bar p-0! pl-4!'">
                <ng-template pTemplate="start">
                    <img [routerLink]="['/']" src="/assets/images/logo_circle.png" class="logo"
                         draggable="false"
                         [style.width.px]="60" alt="logo"/>
                    <span [routerLink]="['/']" class="logo">Kronos ORM</span>
                </ng-template>
                <ng-template pTemplate="item" let-item>
                    <a *ngIf="item.root" pRipple [routerLink]="item.routerLink" routerLinkActive="p-menuitem-active"
                       class="flex items-center p-menuitem-link cursor-pointer px-4 py-2 overflow-hidden relative font-semibold text-lg">
                        <i [ngClass]="item.icon"></i>
                        <span class="mx-2">{{ item.label | transloco }}</span>
                    </a>
                    <a *ngIf="!item.root && !item.image" [routerLink]="item.routerLink" pRipple
                       routerLinkActive="p-menuitem-active"
                       class="flex items-center p-4 cursor-pointer mb-2 gap-2">
                    <span
                            class="inline-flex items-center justify-center rounded-full bg-primary text-primary-contrast w-12 h-12">
                        <i [ngClass]="item.icon + ' text-lg'"></i>
                    </span>
                        <span class="inline-flex flex-col gap-1" style="width: calc(100% - 3.5rem)">
                        <span class="font-medium text-lg">{{ item.label | transloco }}</span>
                        <span class="whitespace-nowrap text-ellipsis overflow-hidden text-gray-200">{{ item.subtext | transloco }}</span>
                    </span>
                    </a>
                    <a [routerLink]="item.routerLink" *ngIf="item.image" pRipple routerLinkActive="p-menuitem-active"
                       class="flex flex-col items-start gap-4 p-2">
                        <img [src]="item.image" alt="megamenu-demo" class="w-48"/>
                        <span class="text-gray-200">{{ item.subtext | transloco }}</span>
                        <p-button [label]="item.label | transloco" [outlined]="true"></p-button>
                    </a>
                </ng-template>
                <ng-template pTemplate="end">
                    <p-button
                            styleClass="text-white!"
                            class="mr-6"
                            link
                            (click)="op.toggle($event)"
                            icon="pi pi-language"/>
                    <a href="https://github.com/Kronos-orm/Kronos-orm"
                       target="_blank"
                       [style.height.px]="42"
                       class="mr-6 p-ripple p-element p-button p-component p-button-icon-only p-button-link text-white!">
                        <i class="pi pi-github"></i>
                    </a>
                </ng-template>
            </p-megaMenu>
            <div class="!block md:hidden! flex items-center border-0 menu-bar p-0 pl-6">
                <div>
                    <img [routerLink]="['/']" src="/assets/images/logo_circle.png" class="logo"
                         draggable="false"
                         [style.width.px]="60" alt="logo"/>
                    <span [routerLink]="['/']" class="logo">Kronos ORM</span>
                </div>
                <div [style.margin-left]="'auto'">
                    <p-button link
                              (click)="op.toggle($event)"
                              icon="pi pi-language"/>
                    <a href="https://github.com/Kronos-orm/Kronos-orm"
                       target="_blank"
                       [style.height.px]="42"
                       class="p-ripple p-element p-button p-component p-button-icon-only p-button-link">
                        <i class="pi pi-github"></i>
                    </a>
                    <p-button class="mr-6" icon="pi pi-bars" link
                              (click)="menu.toggle($event)"/>
                </div>
            </div>
            <p-popover #op styleClass="m-0 p-0">
                <ui-list [list]="languages" (onClick)="setLang($event.lang)"/>
            </p-popover>
            <p-popover #menu styleClass="m-0 p-0">
                <ui-list [list]="menus" (onClick)="onSelect($event)"/>
            </p-popover>
        </div>
    `,
    standalone: true,
    styles: [
        `
            .bar-container {
                z-index: 51;
            }

            :host ::ng-deep .p-megamenu-start {
                margin-right: 24px;
            }

            :host ::ng-deep .p-menuitem-active {
                color: rgba(255, 255, 255, 0.87);
                background: rgba(255, 255, 255, 0.2);
            }

            :host ::ng-deep a {
                text-decoration: none;
            }

            :host ::ng-deep .menu-bar {
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
                padding: 15px;
            }

            .logo:hover {
                filter: grayscale(100%);
                opacity: 0.8;
                transition: .1s ease-in-out;
            }
        `
    ],
    providers: [MessageService]
})
export class LayoutMenuBarComponent implements OnInit {
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
                            label: "BLOG",
                            icon: 'pi pi-home',
                            routerLink: '/blog'
                        }]
                    }],
                    [{
                        items: [{
                            image: 'https://cdn.leinbo.com/assets/images/kronos/code-cover.jpg',
                            label: 'COMING_SOON',
                            subtext: 'CODE_GENERATOR',
                        }]
                    }],
                ]
            },
            {
                label: 'DISCUSSION',
                icon: 'pi pi-comments',
                root: true,
                routerLink: 'https://github.com/Kronos-orm/Kronos-orm/discussions'
            }
        ];
        this.translocoService.selectTranslate(["DOCUMENTATION", "CODE_GENERATOR", "DISCUSSION", "BLOG"])
            .subscribe(([documentation, code_generator, discussion, blog]) => {
                this.menus = [
                    {
                        label: documentation,
                        href: `/#/documentation/${this.appService.language}/getting-started/introduce`
                    },
                    {label: code_generator},
                    {label: blog, href: '/#/blog'},
                    {label: discussion, href: 'https://github.com/Kronos-orm/Kronos-orm/discussions'},
                ];
            })
    }

    constructor(private appService: AppService, private translocoService: TranslocoService, private messageService: MessageService) {
    }

    setLang(lang: string) {
        if (this.appService.language === lang) return;
        this.appService.language = lang; // update language
        this.translocoService.setActiveLang(lang);
        window.location.reload();
    }

    languages = [
        {lang: 'zh-CN', label: '简体中文', rightIcon: 'pi pi-globe'},
        {lang: 'en', label: 'English', rightIcon: 'pi pi-globe'}]

    menus = [];

    onSelect(item: ListItem) {
        if (item.href) {
            window.location.href = item.href;
        } else {
            this.messageService.add({
                severity: 'info',
                summary: this.translocoService.translate("COMING_SOON"),
                detail: item.label
            });
        }
    }

    fixed = false

    @HostListener('window:scroll', ['$event'])
    onScroll() {
        this.fixed = window.scrollY > window.innerHeight - 100;
    }
}

import {AppService} from '../../app.service';
import {NgDocLinksDirective} from './ng-doc-links.directive';
import {DocumentationThemeDirective} from './documentation-theme.directive';
import {CopyMarkdownComponent} from './copy-markdown.component';
import {Component, ElementRef, inject} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronos-ng-doc-sidebar/kronos-ng-doc-sidebar.component";
import {RouterLink} from "@angular/router";
import {NgDocThemeToggleComponent} from "@ng-doc/app";
import {NgDocSidebarService} from "@ng-doc/app/services/sidebar";
import {NgDocButtonIconComponent, NgDocIconComponent, NgDocTooltipDirective} from "@ng-doc/ui-kit";
import {WikiComponent} from "../../components/wiki.component";
import {LanguageSwitchComponent} from "../../components/language-switch/language-switch.component";

@Component({
    selector: 'app-documentation',
    standalone: true,
    imports: [
        SharedModule, NgDocLinksDirective, DocumentationThemeDirective, CopyMarkdownComponent,
        KronosNgDocSidebarComponent,
        NgDocThemeToggleComponent,
        NgDocIconComponent,
        NgDocButtonIconComponent,
        NgDocTooltipDirective,
        WikiComponent,
        RouterLink,
        LanguageSwitchComponent
    ],
    templateUrl: './documentation.component.html',
    styleUrl: './documentation.component.css'
})
export class DocumentationComponent {
    wikiMode = typeof window !== 'undefined' && window.frames.length !== parent.frames.length;
    sidebarCollapsed = false;
    private readonly sidebarService = inject(NgDocSidebarService);
    constructor(public elementRef: ElementRef, private app: AppService) {}

    get sidebarToggleTooltip(): string {
        if (this.sidebarService.isMobile) {
            return this.app.language === 'zh-CN' ? '打开侧栏' : 'Open sidebar';
        }

        return this.app.language === 'zh-CN' ? (this.sidebarCollapsed ? '显示侧栏' : '隐藏侧栏') : (this.sidebarCollapsed ? 'Show sidebar' : 'Hide sidebar');
    }

    toggleSidebar(): void {
        if (this.sidebarService.isMobile) {
            this.sidebarService.toggle();
            return;
        }

        this.sidebarCollapsed = !this.sidebarCollapsed;
    }


}

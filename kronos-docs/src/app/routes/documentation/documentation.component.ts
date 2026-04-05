import {Component, DestroyRef, ElementRef, inject, OnDestroy} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {KronosNgDocSidebarComponent} from "../../components/kronos-ng-doc-sidebar/kronos-ng-doc-sidebar.component";
import {AppService} from "../../app.service";
import {TranslocoService} from "@jsverse/transloco";
import {NavigationEnd, Router, RouterLink} from "@angular/router";
import {NgDocThemeToggleComponent} from "@ng-doc/app";
import {NgDocThemeService} from "@ng-doc/app/services/theme";
import {NgDocButtonIconComponent, NgDocIconComponent, NgDocTooltipDirective} from "@ng-doc/ui-kit";
import {WikiComponent} from "../../components/wiki.component";
import {Popover} from "primeng/popover";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {filter} from "rxjs";
import TurndownService from 'turndown';
import {gfm} from 'turndown-plugin-gfm';

@Component({
    selector: 'app-documentation',
    standalone: true,
    imports: [
        SharedModule,
        KronosNgDocSidebarComponent,
        NgDocThemeToggleComponent,
        NgDocIconComponent,
        NgDocButtonIconComponent,
        NgDocTooltipDirective,
        WikiComponent,
        RouterLink,
        Popover
    ],
    templateUrl: './documentation.component.html',
    styleUrl: './documentation.component.css'
})
export class DocumentationComponent implements OnDestroy {
    wikiMode = false;
    sidebarCollapsed = false;

    private destroyRef = inject(DestroyRef);
    private themeService = inject(NgDocThemeService);
    private copyObserver: MutationObserver | null = null;
    private turndown: TurndownService;

    constructor(
        private appService: AppService,
        private translocoService: TranslocoService,
        private router: Router,
        public elementRef: ElementRef) {
        this.wikiMode = window.frames.length !== parent.frames.length;

        // Setup turndown (HTML → Markdown)
        this.turndown = new TurndownService({headingStyle: 'atx', codeBlockStyle: 'fenced'});
        this.turndown.use(gfm);
        // Skip ng-doc UI elements
        this.turndown.remove(['ng-doc-breadcrumb', 'ng-doc-page-navigation', 'ng-doc-toc', 'style', 'script'] as any[]);

        // Apply current theme immediately on init
        this.applyDarkClass(this.themeService.currentTheme);

        // Sync NgDoc theme → .dark class on <html> so PrimeNG follows along
        this.themeService.themeChanges()
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(theme => this.applyDarkClass(theme));

        // Inject copy-markdown button on every navigation
        this.router.events
            .pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.injectCopyButton());

        // Also try on first load
        this.injectCopyButton();
    }

    ngOnDestroy(): void {
        // Restore dark class when leaving documentation, since all other pages are always dark
        document.documentElement.classList.add('dark');
        this.copyObserver?.disconnect();
    }

    get language(): string {
        return this.appService.language;
    }

    private applyDarkClass(theme: string | null) {
        const isDark = theme === 'dark' ||
            (theme === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches);
        document.documentElement.classList.toggle('dark', isDark);
    }

    private injectCopyButton(): void {
        this.copyObserver?.disconnect();
        this.copyObserver = new MutationObserver(() => {
            const controls = document.querySelector('.ng-doc-page-controls');
            if (!controls) return;
            if (controls.querySelector('.kronos-copy-md-btn')) return;

            const btn = document.createElement('a');
            btn.className = 'kronos-copy-md-btn';
            btn.setAttribute('ng-doc-button-icon', '');
            btn.style.cssText = 'cursor:pointer; display:inline-flex; align-items:center; justify-content:center; width:32px; height:32px; border-radius:50%; color:var(--ng-doc-text); position:relative;';
            btn.setAttribute('data-tooltip', 'Copy as Markdown');
            btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;

            btn.addEventListener('click', async () => {
                try {
                    const page = document.querySelector('article');
                    if (!page) return;
                    const clone = page.cloneNode(true) as HTMLElement;
                    clone.querySelectorAll('.ng-doc-page-controls').forEach(el => el.remove());
                    const md = this.turndown.turndown(clone.innerHTML);
                    await navigator.clipboard.writeText(md);

                    // Visual feedback
                    btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--ng-doc-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
                    btn.setAttribute('data-tooltip', 'Copied!');
                    setTimeout(() => {
                        btn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`;
                        btn.setAttribute('data-tooltip', 'Copy as Markdown');
                    }, 2000);
                } catch {
                    btn.setAttribute('data-tooltip', 'Copy failed');
                }
            });

            controls.insertBefore(btn, controls.firstChild);
            this.copyObserver?.disconnect();
        });

        this.copyObserver.observe(document.body, {childList: true, subtree: true});
    }

    async setLang(lang: string) {
        this.appService.language = lang; // update language
        this.translocoService.setActiveLang(lang);
        const newUrl = `/documentation/${lang}/${this.router.url.split("/").slice(3).join("/")}`;
        await this.router.navigate([newUrl]);
    }
}

import {AfterViewInit, Directive, ElementRef, inject, OnDestroy} from '@angular/core';
import {Router} from '@angular/router';
import {documentationAnchorUrl, repositorySourceUrl} from './doc-links';

/** Compatibility boundary for controls rendered asynchronously by NgDoc. */
@Directive({selector: '[kronosDocLinks]', standalone: true})
export class NgDocLinksDirective implements AfterViewInit, OnDestroy {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly router = inject(Router);
    private observer?: MutationObserver;
    private readonly listeners = new AbortController();

    ngAfterViewInit(): void {
        // A single delegated listener also handles headings inserted by later route renders.
        this.host.nativeElement.addEventListener('click', event => {
            if (!(event.target instanceof Element)) return;
            const button = event.target.closest('ng-doc-heading-anchor button');
            if (!button) return;
            const anchor = button.closest('ng-doc-heading-anchor');
            const id = anchor?.closest('h1, h2, h3, h4, h5, h6')?.id || anchor?.getAttribute('anchor');
            if (!id) return;
            event.preventDefault(); event.stopImmediatePropagation();
            void navigator.clipboard.writeText(documentationAnchorUrl(window.location.origin, window.location.pathname,
                this.router.url.split('#', 1)[0] || '/', id));
        }, {capture: true, signal: this.listeners.signal});
        this.patchRepositorySourceLinks();
        this.observer = new MutationObserver(() => this.patchRepositorySourceLinks());
        this.observer.observe(this.host.nativeElement, {childList: true, subtree: true});
    }

    ngOnDestroy(): void { this.observer?.disconnect(); this.listeners.abort(); }

    private patchRepositorySourceLinks(): void {
        this.host.nativeElement.querySelectorAll<HTMLAnchorElement>('.ng-doc-page-controls a[href]').forEach(link => {
            const href = link.getAttribute('href');
            if (!href) return;
            const patched = repositorySourceUrl(href);
            if (patched !== href) link.setAttribute('href', patched);
        });
    }
}

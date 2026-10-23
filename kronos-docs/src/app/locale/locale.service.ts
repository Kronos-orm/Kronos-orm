import {DOCUMENT} from '@angular/common';
import {DestroyRef, Injectable, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NavigationEnd, Router} from '@angular/router';
import {TranslocoService} from '@jsverse/transloco';
import {filter} from 'rxjs';
import {isLocale, Locale, localizedUrl, resolveLocale, routeLocale} from './locale-policy';

@Injectable({providedIn: 'root'})
export class LocaleService {
    private readonly document = inject(DOCUMENT);
    private readonly router = inject(Router);
    private readonly translations = inject(TranslocoService);
    private readonly selected = signal<Locale>('en');
    readonly current = this.selected.asReadonly();

    constructor() {
        // Before Angular's initial navigation completes, Router.url is still '/'.
        // Read the actual deep link once so a stored homepage preference never wins that frame.
        const initialHash = this.document.defaultView?.location.hash.slice(1) ?? '';
        this.synchronize(this.router.url === '/' && routeLocale(initialHash) ? initialHash : this.router.url);
        this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            takeUntilDestroyed(inject(DestroyRef))).subscribe(event => this.synchronize(event.urlAfterRedirects));
    }

    setLanguage(locale: Locale): void {
        this.apply(locale);
        const next = localizedUrl(this.router.url, locale);
        if (next !== this.router.url) void this.router.navigateByUrl(next);
    }

    private synchronize(url: string): void {
        let preference: string | null = null;
        try { preference = this.document.defaultView?.localStorage.getItem('language') ?? null; } catch { /* Storage may be disabled. */ }
        this.apply(resolveLocale(url, preference, this.document.defaultView?.navigator.language ?? 'en'));
    }

    private apply(locale: Locale): void {
        if (!isLocale(locale)) return;
        this.selected.set(locale);
        this.translations.setActiveLang(locale);
        this.document.documentElement.lang = locale;
        try { this.document.defaultView?.localStorage.setItem('language', locale); } catch { /* Language remains available in memory. */ }
    }
}

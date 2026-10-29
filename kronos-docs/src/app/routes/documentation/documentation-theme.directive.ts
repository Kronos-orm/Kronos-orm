import {Directive, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgDocThemeService} from '@ng-doc/app/services/theme';
import {DOCUMENT} from '@angular/common';

@Directive({selector: '[kronosDocumentationTheme]', standalone: true})
export class DocumentationThemeDirective {
    constructor() {
        const document = inject(DOCUMENT);
        const view = document.defaultView;
        const theme = inject(NgDocThemeService);
        const destroy = inject(DestroyRef);
        try {
            if (view?.localStorage.getItem('ng-doc-theme-id') === null) {
                view.localStorage.setItem('ng-doc-theme-id', 'dark');
                document.documentElement.setAttribute('data-theme', 'dark');
            }
        } catch { /* Browser storage is optional. */ }
        const apply = (value: string | null) => document.documentElement.classList.toggle('dark',
            value === 'dark' || value === 'auto' && !!view?.matchMedia('(prefers-color-scheme: dark)').matches);
        apply(theme.currentTheme);
        theme.themeChanges().pipe(takeUntilDestroyed(destroy)).subscribe(apply);
        destroy.onDestroy(() => document.documentElement.classList.add('dark'));
    }
}

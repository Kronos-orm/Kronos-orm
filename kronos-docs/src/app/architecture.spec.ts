import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {DocSearchEngine} from './doc-search-engine';
import {Component, CUSTOM_ELEMENTS_SCHEMA, signal} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {NavigationEnd, Router} from '@angular/router';
import {TranslocoService} from '@jsverse/transloco';
import {Subject} from 'rxjs';
import {LocaleService} from './locale/locale.service';
import {NgDocLinksDirective} from './routes/documentation/ng-doc-links.directive';
import {CopyMarkdownComponent} from './routes/documentation/copy-markdown.component';
import {AppService} from './app.service';

@Component({standalone: true, imports: [NgDocLinksDirective], schemas: [CUSTOM_ELEMENTS_SCHEMA],
    template: '<section kronosDocLinks><div class="ng-doc-page-controls"><a href="https://github.com/Kronos-orm/Kronos-orm/edit/main/src/app/docs/en/page.md">Source</a></div><h2 id="heading"><ng-doc-heading-anchor><button>Anchor</button></ng-doc-heading-anchor></h2></section>'})
class LinksHost {}

describe('documentation architecture', () => {
    let events: Subject<NavigationEnd>;
    let originalUrl: string;
    let router: {url: string; events: Subject<NavigationEnd>; navigateByUrl: jasmine.Spy};
    let translations: {setActiveLang: jasmine.Spy};
    beforeEach(() => {
        originalUrl = location.href;
        events = new Subject();
        router = {url: '/documentation/zh-CN/getting-started/kmp?mode=all#native', events, navigateByUrl: jasmine.createSpy().and.resolveTo(true)};
        translations = {setActiveLang: jasmine.createSpy()};
        TestBed.configureTestingModule({providers: [
            {provide: Router, useValue: router}, {provide: TranslocoService, useValue: translations}
        ]});
        localStorage.setItem('language', 'en');
    });
    afterEach(() => { TestBed.resetTestingModule(); localStorage.removeItem('language'); history.replaceState(null, '', originalUrl); });

    it('uses the direct-link locale and keeps URL, storage, html lang and translation state together', () => {
        const locale = TestBed.inject(LocaleService);
        expect(locale.current()).toBe('zh-CN');
        expect(document.documentElement.lang).toBe('zh-CN');
        expect(localStorage.getItem('language')).toBe('zh-CN');
        expect(translations.setActiveLang).toHaveBeenCalledWith('zh-CN');
        locale.setLanguage('en');
        expect(router.navigateByUrl).toHaveBeenCalledWith('/documentation/en/getting-started/kmp?mode=all#native');
        events.next(new NavigationEnd(1, '/documentation/en/query/select', '/documentation/en/query/select'));
        expect(locale.current()).toBe('en');
    });

    for (const language of ['en', 'zh-CN']) {
        it(`uses the ${language} hash deep link before initial Angular navigation`, () => {
            router.url = '/';
            localStorage.setItem('language', language === 'en' ? 'zh-CN' : 'en');
            history.replaceState(null, '', `#/documentation/${language}/getting-started/kmp`);
            const locale = TestBed.inject(LocaleService);
            expect(locale.current()).toBe(language);
            expect(translations.setActiveLang.calls.allArgs()).toEqual([[language]]);
        });
    }

    it('releases its navigation subscription when the application injector is destroyed', () => {
        TestBed.inject(LocaleService);
        TestBed.resetTestingModule();
        const count = translations.setActiveLang.calls.count();
        events.next(new NavigationEnd(2, '/documentation/en/query/select', '/documentation/en/query/select'));
        expect(translations.setActiveLang.calls.count()).toBe(count);
    });

    it('patches only its own NgDoc controls and aborts clipboard listeners on destroy', () => {
        const copy = spyOn(navigator.clipboard, 'writeText').and.resolveTo();
        const fixture = TestBed.createComponent(LinksHost); fixture.detectChanges();
        const link = fixture.nativeElement.querySelector('a');
        expect(link.href).toBe('https://github.com/Kronos-orm/Kronos-orm/edit/main/kronos-docs/src/app/docs/en/page.md');
        const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
        button.click();
        expect(copy).toHaveBeenCalledWith(`${location.origin}${location.pathname}#/documentation/zh-CN/getting-started/kmp?mode=all#heading`);
        const calls = copy.calls.count(); fixture.destroy(); button.click();
        expect(copy.calls.count()).toBe(calls);
    });

    it('re-ranks cached search results immediately when the application language changes', () => {
        const language = signal<'en' | 'zh-CN'>('en');
        TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
        TestBed.overrideProvider(LocaleService, {useValue: {current: language.asReadonly()}});
        const engine = TestBed.runInInjectionContext(() => new DocSearchEngine());
        const http = TestBed.inject(HttpTestingController);
        let routes: string[] = [];
        const subscription = engine.search('SQLite').subscribe(results => routes = results.map(result => result.index.route));
        TestBed.tick();
        http.expectOne('assets/ng-doc/indexes.json').flush([
            {title: 'SQLite', route: '/documentation/en/database/sqlite', content: 'SQLite'},
            {title: 'SQLite', route: '/documentation/zh-CN/database/sqlite', content: 'SQLite'}
        ]);
        expect(routes).toEqual(['/documentation/en/database/sqlite']);
        language.set('zh-CN'); TestBed.tick();
        expect(routes).toEqual(['/documentation/zh-CN/database/sqlite']);
        http.expectNone('assets/ng-doc/indexes.json'); http.verify(); subscription.unsubscribe();
    });

    it('copies actual article headings and fenced code while excluding page controls', async () => {
        TestBed.overrideProvider(AppService, {useValue: {language: 'en'}});
        const copy = spyOn(navigator.clipboard, 'writeText').and.resolveTo();
        const fixture = TestBed.createComponent(CopyMarkdownComponent);
        const container = document.createElement('div');
        container.innerHTML = '<article><h2>Example</h2><pre><code>val answer = 42</code></pre><div class="ng-doc-page-controls">Source buttons</div></article>';
        fixture.componentRef.setInput('container', container); fixture.detectChanges();
        await fixture.componentInstance.copy();
        const markdown = copy.calls.mostRecent().args[0];
        expect(markdown).toBe('## Example\n\n```\nval answer = 42\n```');
        expect(markdown).not.toContain('Source buttons'); expect(fixture.componentInstance.label()).toBe('Copied');
        fixture.destroy();
    });
});

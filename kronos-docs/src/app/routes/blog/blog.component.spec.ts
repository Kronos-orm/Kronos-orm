import {HttpClient, provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {TestBed} from '@angular/core/testing';
import {provideRouter, Router, withHashLocation} from '@angular/router';
import {provideMarkdown} from 'ngx-markdown';
import {AppService} from '../../app.service';
import {BlogComponent} from './blog.component';

describe('Blog article rendering', () => {
    it('keeps the table of contents after Markdown loads and follows documentation links in hash mode', async () => {
        await TestBed.configureTestingModule({imports: [BlogComponent], providers: [
            provideHttpClient(), provideHttpClientTesting(), provideMarkdown({loader: HttpClient}),
            provideRouter([{path: '**', component: BlogComponent}], withHashLocation()),
            {provide: AppService, useValue: {language: 'zh-CN'}}
        ]}).compileComponents();
        const router = TestBed.inject(Router);
        await router.navigateByUrl('/blog?blog=kotlin-multiplatform-support');
        const fixture = TestBed.createComponent(BlogComponent);
        fixture.detectChanges();
        const http = TestBed.inject(HttpTestingController);
        http.expectOne('/assets/blogs/zh-CN/kotlin-multiplatform-support.md').flush(
            '# KMP\n\n## 共享模型\n\n[KEFS](/documentation/zh-CN/resources/kefs)\n\n```kotlin\nval name = "Kotlin"\n```'
        );
        await fixture.whenStable(); fixture.detectChanges();
        const body: HTMLElement = fixture.nativeElement;
        const toc = body.querySelector<HTMLAnchorElement>('.article-toc a')!;
        expect(toc.textContent?.trim()).toBe('01共享模型');
        expect(toc.getAttribute('href')).toBe('#/blog?blog=kotlin-multiplatform-support#%E5%85%B1%E4%BA%AB%E6%A8%A1%E5%9E%8B');
        const link = body.querySelector<HTMLAnchorElement>('markdown a')!;
        expect(link.getAttribute('href')).toBe('#/documentation/zh-CN/resources/kefs');
        const navigate = spyOn(router, 'navigateByUrl').and.resolveTo(true);
        link.click(); await fixture.whenStable();
        expect(navigate).toHaveBeenCalledOnceWith('/documentation/zh-CN/resources/kefs');
        const clipboard = spyOn(navigator.clipboard, 'writeText').and.resolveTo();
        body.querySelector<HTMLButtonElement>('button[data-copy-code]')!.click();
        await fixture.whenStable();
        expect(clipboard).toHaveBeenCalledOnceWith('val name = "Kotlin"\n');
        expect(body.querySelector('button[data-copy-code]')!.textContent).toBe('已复制');
        fixture.componentInstance.prepareArticle();
        expect(body.querySelectorAll('.article-code-toolbar').length).toBe(1);
        http.verify(); fixture.destroy();
    });
});

import {CommonModule, Location} from '@angular/common';
import {Component, ElementRef, signal, ViewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {MarkdownComponent} from 'ngx-markdown';
import {AppService} from '../../app.service';
import {applicationRoute} from '../../application-links';

type LocalizedText = {
    zh: string;
    en: string;
};

type BlogEntry = {
    slug: string;
    image: string;
    title: LocalizedText;
    copy: LocalizedText;
    tag: LocalizedText;
};

@Component({
    selector: 'blog',
    imports: [
        CommonModule,
        RouterLink,
        MarkdownComponent
    ],
    templateUrl: './blog.component.html',
    standalone: true,
    styleUrl: './blog.component.css'
})
export class BlogComponent {
    blog?: string;
    readMinutes = 1;
    readonly headings = signal<{id: string; title: string}[]>([]);
    @ViewChild('articleBody', {read: ElementRef}) private articleBody?: ElementRef<HTMLElement>;

    readonly blogs: BlogEntry[] = [
        {
            slug: 'kotlin-multiplatform-support',
            image: '/assets/images/features/img-1.png',
            title: {
                zh: 'Kotlin 多平台支持',
                en: 'Kotlin multiplatform support'
            },
            copy: {
                zh: '在 JVM、Android/JVM 和 Kotlin/Native 间共享模型与查询，并接入平台数据库连接。',
                en: 'Share models and queries across JVM, Android/JVM and Kotlin/Native, with platform database connections.'
            },
            tag: {
                zh: '多平台',
                en: 'Multiplatform'
            }
        },
        {
            slug: 'code-generation-support',
            image: '/assets/images/features/img-3.png',
            title: {
                zh: 'Kronos 代码生成器',
                en: 'Code generation framework'
            },
            copy: {
                zh: '根据数据库表结构生成 Kotlin ORM 类和常用 Kotlin Class 的配置方式。',
                en: 'Generate Kotlin ORM classes and common Kotlin classes from database table structures.'
            },
            tag: {
                zh: '插件',
                en: 'Plugin'
            }
        },
        {
            slug: 'kefs-editor-support',
            image: '/assets/images/features/img-2.png',
            title: {
                zh: '使用 KEFS 支持 Kronos 编辑器分析',
                en: 'Kronos editor support with KEFS'
            },
            copy: {
                zh: '让 IDEA K2 分析理解 Kronos 生成成员、生成投影和 DSL diagnostics。',
                en: 'Make IDEA K2 analysis understand Kronos generated members, generated projections, and DSL diagnostics.'
            },
            tag: {
                zh: 'IDE',
                en: 'IDE'
            }
        }
    ];

    constructor(public appService: AppService, activatedRoute: ActivatedRoute, private router: Router,
                private location: Location) {
        activatedRoute.queryParams.pipe(takeUntilDestroyed()).subscribe(params => {
            this.blog = this.blogs.some(entry => entry.slug === params['blog']) ? params['blog'] : undefined;
        });
    }

    get language(): string {
        return this.appService.language;
    }

    get isZh(): boolean {
        return this.language === 'zh-CN';
    }

    get docsUrl(): string {
        return `/documentation/${this.language}/getting-started/introduce`;
    }

    get quickStartUrl(): string {
        return `/documentation/${this.language}/getting-started/quick-start`;
    }

    get articleTitle(): string {
        const item = this.blogs.find(blog => blog.slug === this.blog);
        return item ? this.text(item.title) : 'Kronos Blog';
    }

    get articleTag(): string {
        const entry = this.blogs.find(item => item.slug === this.blog);
        return entry ? this.text(entry.tag) : 'Kronos Blog';
    }

    onArticleLoad(markdown: string): void {
        this.readMinutes = Math.max(1, Math.ceil(this.isZh ? markdown.length / 650 : markdown.split(/\s+/).length / 220));
    }

    headingUrl(id: string): string {
        return this.location.prepareExternalUrl(`${this.router.url.split('#')[0]}#${encodeURIComponent(id)}`);
    }

    prepareArticle(): void {
        const body = this.articleBody?.nativeElement;
        if (!body) return;
        const used = new Set<string>();
        this.headings.set(Array.from(body.querySelectorAll('h2')).map((heading, index) => {
            const title = heading.textContent?.trim() || '';
            const base = title.toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '') || `section-${index + 1}`;
            let id = base;
            let suffix = 2;
            while (used.has(id)) id = `${base}-${suffix++}`;
            used.add(id);
            heading.id = id;
            return {id, title};
        }));
        for (const link of Array.from(body.querySelectorAll('a[href]'))) {
            const route = applicationRoute(link.getAttribute('href')!, location.origin, this.router.url);
            if (route) link.setAttribute('href', this.location.prepareExternalUrl(route));
            else if (/^https?:\/\//.test(link.getAttribute('href')!)) {
                link.setAttribute('target', '_blank');
                link.setAttribute('rel', 'noopener noreferrer');
            }
        }
        for (const pre of Array.from(body.querySelectorAll('pre'))) {
            if (pre.parentElement?.classList.contains('article-code')) continue;
            const code = pre.querySelector('code');
            if (!code) continue;
            const wrapper = document.createElement('div');
            wrapper.className = 'article-code';
            const toolbar = document.createElement('div');
            toolbar.className = 'article-code-toolbar';
            const language = document.createElement('span');
            language.textContent = Array.from(code.classList).find(value => value.startsWith('language-'))?.slice(9) || 'code';
            const button = document.createElement('button');
            button.type = 'button';
            button.dataset['copyCode'] = '';
            button.textContent = this.isZh ? '复制代码' : 'Copy code';
            button.setAttribute('aria-live', 'polite');
            toolbar.append(language, button);
            pre.before(wrapper);
            wrapper.append(toolbar, pre);
        }
    }

    async onArticleClick(event: MouseEvent): Promise<void> {
        if (!(event.target instanceof Element)) return;
        const copy = event.target.closest<HTMLButtonElement>('button[data-copy-code]');
        if (copy) {
            const code = copy.closest('.article-code')?.querySelector('code')?.textContent;
            if (code != null) {
                try {
                    await navigator.clipboard.writeText(code);
                    copy.textContent = this.isZh ? '已复制' : 'Copied';
                } catch { copy.textContent = this.isZh ? '复制失败' : 'Copy failed'; }
            }
            return;
        }
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.altKey || event.shiftKey) return;
        const link = event.target.closest<HTMLAnchorElement>('a[href]');
        if (!link || link.target === '_blank' || link.hasAttribute('download')) return;
        const route = applicationRoute(link.getAttribute('href')!, location.origin, this.router.url);
        if (route) {
            event.preventDefault();
            await this.router.navigateByUrl(route);
        }
    }

    text(value: LocalizedText): string {
        return this.isZh ? value.zh : value.en;
    }

    setLanguage(language: string): void {
        this.appService.language = language;
    }

    backToList(): void {
        this.blog = undefined;
        this.router.navigateByUrl('/blog');
    }
}

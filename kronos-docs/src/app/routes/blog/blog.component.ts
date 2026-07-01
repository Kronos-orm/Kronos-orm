import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {MarkdownComponent} from 'ngx-markdown';
import {AppService} from '../../app.service';

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

    readonly blogs: BlogEntry[] = [
        {
            slug: 'kotlin-multiplatform-support',
            image: '/assets/images/features/img-1.png',
            title: {
                zh: 'Kotlin 多平台支持',
                en: 'Kotlin multiplatform support'
            },
            copy: {
                zh: 'Kronos 当前对 Kotlin 多平台的支持情况，以及后续开发和发布计划。',
                en: 'Current Kotlin multiplatform support in Kronos, plus development and release plans.'
            },
            tag: {
                zh: '路线图',
                en: 'Roadmap'
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
        }
    ];

    constructor(public appService: AppService, activatedRoute: ActivatedRoute, private router: Router) {
        activatedRoute.queryParams.subscribe(params => {
            this.blog = params['blog'];
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

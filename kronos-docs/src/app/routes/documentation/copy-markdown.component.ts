import {Component, Input, inject, signal} from '@angular/core';
import {AppService} from '../../app.service';
import TurndownService from 'turndown';
import {gfm} from 'turndown-plugin-gfm';

@Component({selector: 'kronos-copy-markdown', standalone: true, template: `
    <button type="button" class="copy-markdown" (click)="copy()" [attr.aria-label]="label()" [title]="label()">{{label()}}</button>`,
    styles: [`.copy-markdown {cursor:pointer;color:var(--ng-doc-text);background:transparent;border:1px solid var(--ng-doc-base-4);border-radius:6px;padding:6px 10px;font-size:12px}`]})
export class CopyMarkdownComponent {
    @Input({required: true}) container!: HTMLElement;
    private readonly app = inject(AppService);
    private readonly status = signal<'ready' | 'copied' | 'failed'>('ready');
    label(): string {
        const zh = this.app.language === 'zh-CN';
        return this.status() === 'copied' ? (zh ? '已复制' : 'Copied') : this.status() === 'failed' ?
            (zh ? '复制失败' : 'Copy failed') : (zh ? '复制 Markdown' : 'Copy Markdown');
    }
    async copy(): Promise<void> {
        const article = this.container.querySelector('article');
        if (!article) return;
        try {
            const clone = article.cloneNode(true) as HTMLElement;
            clone.querySelectorAll('.ng-doc-page-controls, ng-doc-breadcrumb, ng-doc-page-navigation, ng-doc-toc, style, script').forEach(node => node.remove());
            const converter = new TurndownService({headingStyle: 'atx', codeBlockStyle: 'fenced'});
            converter.use(gfm);
            await navigator.clipboard.writeText(converter.turndown(clone.innerHTML));
            this.status.set('copied');
        } catch { this.status.set('failed'); }
    }
}

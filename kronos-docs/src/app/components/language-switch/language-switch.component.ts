import {CommonModule} from '@angular/common';
import {Component, Input} from '@angular/core';
import {Router} from '@angular/router';
import {TranslocoService} from '@jsverse/transloco';
import {Popover} from 'primeng/popover';
import {AppService} from '../../app.service';

type LanguageCode = 'zh-CN' | 'en';

type LanguageOption = {
    lang: LanguageCode;
    label: string;
};

@Component({
    selector: 'kronos-language-switch',
    standalone: true,
    imports: [
        CommonModule,
        Popover
    ],
    templateUrl: './language-switch.component.html',
    styleUrl: './language-switch.component.css'
})
export class LanguageSwitchComponent {
    @Input() appendTo: HTMLElement | string | null = null;

    readonly languages: LanguageOption[] = [
        {lang: 'zh-CN', label: '简体中文'},
        {lang: 'en', label: 'English'}
    ];

    constructor(
        private appService: AppService,
        private translocoService: TranslocoService,
        private router: Router
    ) {
    }

    get language(): LanguageCode {
        return this.appService.language === 'zh-CN' ? 'zh-CN' : 'en';
    }

    get label(): string {
        return this.languages.find(item => item.lang === this.language)?.label ?? 'English';
    }

    setLanguage(language: LanguageCode): void {
        if (this.language === language) {
            return;
        }

        this.appService.language = language;
        this.translocoService.setActiveLang(language);

        if (this.router.url.startsWith('/documentation/')) {
            const docPath = this.router.url.split('/').slice(3).join('/') || 'getting-started/introduce';
            void this.router.navigate([`/documentation/${language}/${docPath}`]);
        }
    }
}

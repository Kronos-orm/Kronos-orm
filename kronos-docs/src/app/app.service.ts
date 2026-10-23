import {Injectable, inject} from '@angular/core';
import {LocaleService} from './locale/locale.service';
import {isLocale} from './locale/locale-policy';

/** Application facade retained for shared homepage and blog components. */
@Injectable({providedIn: 'root'})
export class AppService {
    private readonly locale = inject(LocaleService);
    get language(): string { return this.locale.current(); }
    set language(value: string) { if (isLocale(value)) this.locale.setLanguage(value); }
}

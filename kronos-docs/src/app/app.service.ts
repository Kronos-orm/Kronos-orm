import {Injectable} from "@angular/core";

@Injectable()
export class AppService {
  _language = 'en';

  get language(): string {
    if (typeof window !== 'undefined') {
      if (localStorage.getItem('language')) {
        this._language = localStorage.getItem('language')!!;
      } else {
        window.localStorage.setItem('language', navigator.language.includes("zh") ? "zh-CN" : "en");
      }
    }
    return this._language;
  }

  set language(value: string) {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem('language', value);
    }
    this._language = value;
  }
}

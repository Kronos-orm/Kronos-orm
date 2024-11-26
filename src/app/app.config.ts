import {
    provideNgDocApp,
    provideSearchEngine,
    providePageSkeleton,
    NG_DOC_DEFAULT_PAGE_SKELETON,
    provideMainPageProcessor,
    NG_DOC_DEFAULT_PAGE_PROCESSORS
} from "@ng-doc/app";
import {provideNgDocContext} from "@ng-doc/generated";
import {provideMermaid} from '@ng-doc/app';
import {provideHttpClient, withInterceptorsFromDi, withFetch} from "@angular/common/http";
import {ApplicationConfig, isDevMode, SecurityContext} from '@angular/core';
import {provideRouter, withHashLocation, withInMemoryScrolling} from '@angular/router';
import {routes} from './app.routes';
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {provideTransloco} from "@jsverse/transloco";
import {TranslocoHttpLoader} from "./TranslocoHttpLoader";
import {DocSearchEngine} from "./doc-search-engine";
import {MARKED_OPTIONS, provideMarkdown} from "ngx-markdown";

export const appConfig: ApplicationConfig = {
    providers: [
        provideAnimationsAsync(),
        provideHttpClient(withInterceptorsFromDi()),
        provideRouter(routes, withInMemoryScrolling({
            scrollPositionRestoration: "enabled",
            anchorScrolling: "enabled",
        }), withHashLocation()),
        provideHttpClient(withInterceptorsFromDi(), withFetch()),
        provideNgDocContext(),
        provideMermaid(),
        provideNgDocApp(),
        provideSearchEngine(DocSearchEngine),
        providePageSkeleton(NG_DOC_DEFAULT_PAGE_SKELETON),
        provideMainPageProcessor(NG_DOC_DEFAULT_PAGE_PROCESSORS),
        provideTransloco({
            config: {
                availableLangs: ['en', 'zh-CN'],
                defaultLang: 'en',
                // Remove this option if your application doesn't support changing language in runtime.
                reRenderOnLangChange: false,
                prodMode: !isDevMode(),
            },
            loader: TranslocoHttpLoader
        }),
        provideMarkdown({
            markedOptions: {
                provide: MARKED_OPTIONS,
                useValue: {
                    gfm: true,
                    breaks: false,
                    pedantic: false,
                    smartLists: true, // enable smartLists
                    smartypants: false,
                },
            },
            sanitize: SecurityContext.NONE // disable sanitization
        })
    ]
};

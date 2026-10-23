import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {NgDocSearchEngine, NgDocSearchResult} from '@ng-doc/app';
import {NgDocPageIndex} from '@ng-doc/core';
import {Observable, combineLatest, defer} from 'rxjs';
import {toObservable} from '@angular/core/rxjs-interop';
import {map, shareReplay} from 'rxjs/operators';
import {LocaleService} from './locale/locale.service';
import {rankDocumentation} from './search-policy';

@Injectable()
export class DocSearchEngine extends NgDocSearchEngine {
    private readonly locale = inject(LocaleService);
    private readonly http = inject(HttpClient);
    private readonly language = toObservable(this.locale.current);
    readonly indexes = defer(() => this.http.get<NgDocPageIndex[]>('assets/ng-doc/indexes.json'))
        .pipe(shareReplay({bufferSize: 1, refCount: false}));
    search(query: string): Observable<NgDocSearchResult[]> {
        return combineLatest([this.indexes, this.language]).pipe(map(([indexes, language]) => rankDocumentation(indexes, query, language)));
    }
}

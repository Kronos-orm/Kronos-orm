import {NgDocSearchEngine, NgDocSearchResult} from '@ng-doc/app';
import {NgDocPageIndex} from '@ng-doc/core';
import {from, Observable} from 'rxjs';
import {map, shareReplay, switchMap} from 'rxjs/operators';

export class DocSearchEngine extends NgDocSearchEngine {
  // Load indexes from assets
  indexes: Observable<NgDocPageIndex[]> =
      from(fetch(`assets/ng-doc/indexes.json`)).pipe(
        switchMap((response: Response) => response.json() as Promise<NgDocPageIndex[]>),
        // Use `shareReplay(1)` to cache the response and avoid making multiple requests
        shareReplay(1),
      )

  search(query: string): Observable<NgDocSearchResult[]> {
    return this.indexes.pipe(
      map((indexes: NgDocPageIndex[]) => this.filterIndexes(indexes, query)),
    );
  }

  private filterIndexes(indexes: NgDocPageIndex[], query: string): NgDocSearchResult[] {
    return (
      indexes
        // Filter indexes by query
        .filter((index: NgDocPageIndex) =>
          index.content?.toLowerCase().includes(query.toLowerCase()) && index.route.split("/")[0] === window.location.hash.split("/")[2]
        )
        // Get first 10 results, you can remove this line to get all results
        // it's recommended to limit the number of results to avoid performance issues
        .slice(0, 10)
        // Map indexes to search results
        .map((index: NgDocPageIndex) => ({
          index,
          // You can provide a list of positions where the query was found in the title
          // then the search component will highlight them
          positions: {
            // Provide the key of the section where the query was found and it's position
            content: [
              {
                start: index.content?.toLowerCase().indexOf(query.toLowerCase()) ?? 0,
                length: query.length,
              },
            ],
          },
        }))
    );
  }
}

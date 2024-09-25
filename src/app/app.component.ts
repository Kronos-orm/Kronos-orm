import {Component, OnInit} from '@angular/core';
import {SharedModule} from "./shared.module";
import {MessageService, PrimeNGConfig} from 'primeng/api';
import {AppService} from "./app.service";
import {TranslocoService} from "@jsverse/transloco";
import {NgxTypedWriterModule} from "ngx-typed-writer";
import {
    NavigationCancel,
    NavigationEnd,
    NavigationError,
    RouteConfigLoadEnd,
    RouteConfigLoadStart,
    Router
} from "@angular/router";
import {Subject, takeUntil} from "rxjs";

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [SharedModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
    providers: [AppService, MessageService]
})
export class AppComponent implements OnInit {
    private unsubscribe$ = new Subject<void>();
    constructor(
        private primengConfig: PrimeNGConfig,
        private translocoService: TranslocoService,
        private appService: AppService,
        private router: Router,
        private messageService: MessageService
    ) {
        router.events.pipe(takeUntil(this.unsubscribe$)).subscribe((evt) => {
            if (!this.isFetching && evt instanceof RouteConfigLoadStart) {
                this.isFetching = true;
            }
            if (evt instanceof NavigationError || evt instanceof NavigationCancel) {
                this.isFetching = false;
                if (evt instanceof NavigationError) {
                    messageService.add({ severity: 'error', summary: 'Page Load Error', detail: '${evt.url} page load error, please refresh the page', closable: false });
                }
                return;
            }
            if (!(evt instanceof NavigationEnd || evt instanceof RouteConfigLoadEnd)) {
                return;
            }
            if (this.isFetching) {
                setTimeout(() => {
                    this.isFetching = false;
                }, 100);
            }
        });
    }

    ngOnDestroy(): void {
        const { unsubscribe$ } = this;
        unsubscribe$.next();
        unsubscribe$.complete();
    }

    title = "Kronos"

    ngOnInit() {
        this.primengConfig.ripple = true;
        this.translocoService.setActiveLang(this.appService.language);
    }
    isFetching = false;
}

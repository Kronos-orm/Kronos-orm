import {Component, OnDestroy, OnInit} from '@angular/core';
import {SharedModule} from "./shared.module";
import {MessageService} from 'primeng/api';
import {AppService} from "./app.service";
import {TranslocoService} from "@jsverse/transloco";
import {
    NavigationCancel,
    NavigationEnd,
    NavigationError,
    RouteConfigLoadEnd,
    RouteConfigLoadStart,
    Router
} from "@angular/router";
import {Subject, takeUntil} from "rxjs";
import {PrimeNG} from "primeng/config";
import {updatePrimaryPalette} from "@primeuix/themes";

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [SharedModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css',
    providers: [AppService, MessageService]
})
export class AppComponent implements OnInit, OnDestroy {
    private unsubscribe$ = new Subject<void>();
    constructor(
        private primengConfig: PrimeNG,
        private translocoService: TranslocoService,
        private appService: AppService,
        router: Router,
        messageService: MessageService
    ) {
        updatePrimaryPalette({
            50: '{violet.50}',
            100: '{violet.100}',
            200: '{violet.200}',
            300: '{violet.300}',
            400: '{violet.400}',
            500: '{violet.500}',
            600: '{violet.600}',
            700: '{violet.700}',
            800: '{violet.800}',
            900: '{violet.900}',
            950: '{violet.950}'
        });
        router.events.pipe(takeUntil(this.unsubscribe$)).subscribe((evt) => {
            if (!this.isFetching && evt instanceof RouteConfigLoadStart) {
                this.isFetching = true;
            }
            if (evt instanceof NavigationError || evt instanceof NavigationCancel) {
                this.isFetching = false;
                if (evt instanceof NavigationError) {
                    messageService.add({ severity: 'error', summary: 'Page Load Error', detail: `${evt.url} page load error, please refresh the page`, closable: false });
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
        this.primengConfig.ripple.set(true);
        this.translocoService.setActiveLang(this.appService.language);
    }
    isFetching = false;
}

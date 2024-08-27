import {Component, OnInit} from '@angular/core';
import {SharedModule} from "./shared.module";
import {PrimeNGConfig} from 'primeng/api';
import {AppService} from "./app.service";
import {TranslocoService} from "@jsverse/transloco";
import {NgxTypedWriterModule} from "ngx-typed-writer";

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [SharedModule, NgxTypedWriterModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.scss',
    providers: [AppService]
})
export class AppComponent implements OnInit {
    constructor(private primengConfig: PrimeNGConfig, private translocoService: TranslocoService, private appService: AppService) {
    }

    title = "Kronos"

    ngOnInit() {
        this.primengConfig.ripple = true;
        this.translocoService.setActiveLang(this.appService.language);
    }

    firstEntry = true;
    codes = [
        '.<span class=\'code-green\'>insert()</span>.<span class=\'code-green\'>execute()</span>',
        '.<span class=\'code-green\'>delete()</span><br/>.<span class=\'code-green\'>where{ </span><span class=\'code-red\'>it.id </span>== 1<span class=\'code-green\'> }</span><br/>.<span class=\'code-green\'>execute()</span>',
        '.<span class=\'code-green\'>select()</span><br/>.<span class=\'code-green\'>where{ </span><span class=\'code-red\'>it.id </span>== 1<span class=\'code-green\'> }</span><br/>.<span class=\'code-green\'>queryList()</span>',
        '.<span class=\'code-green\'>update()</span><br/>.<span class=\'code-green\'>set{ </span><span class=\'code-red\'>it.name </span>== \"name\"<span class=\'code-green\'> }</span><br/>.<span class=\'code-green\'>where{ </span><span class=\'code-red\'>it.id </span>== 1<span class=\'code-green\'> }</span><br/>.<span class=\'code-green\'>execute()</span>',
    ]
}

import {Component} from '@angular/core';
import {SharedModule} from "../../shared.module";
import {AppService} from "../../app.service";
import {LayoutMenuBarComponent} from "../home/components/layout-menu-bar.component";
import {MarkdownComponent} from "ngx-markdown";
import {ActivatedRoute, Router} from "@angular/router";
import {TranslocoPipe} from "@jsverse/transloco";
import {DataViewModule} from "primeng/dataview";
import {FooterComponent} from "../home/components/footer.component";

@Component({
    selector: 'blog',
    imports: [
        SharedModule,
        LayoutMenuBarComponent,
        MarkdownComponent,
        TranslocoPipe,
        DataViewModule,
        FooterComponent
    ],
    templateUrl: `./blog.component.html`,
    standalone: true,
    styleUrl:"./blog.component.scss"
})
export class BlogComponent {
    blog: string;

    constructor(public appService: AppService, activatedRoute: ActivatedRoute, private router: Router) {
        activatedRoute.queryParams.subscribe(params => {
            this.blog = params['blog'];
        })
    }

    blogs = [
        {
            title: 'FEATURE_BLOG_TITLE_1',
            content: 'FEATURE_BLOG_CONTENT_1',
            img: '/assets/images/features/img-1.png',
            blog: 'kotlin-multiplatform-support'
        }
    ]

    backToList() {
        this.blog = undefined;
        this.router.navigateByUrl('/blog');
    }
}

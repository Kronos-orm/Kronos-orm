import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const FirstQueryPage: NgDocPage = {
    title: `第一次查询`,
    mdFile: './index.md',
    route: "first-query",
    category: GettingStartedCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent},
};

export default FirstQueryPage;

import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍查询终端方法和结果形态。
 * @status:info NEW
 */
const ResultMethodsPage: NgDocPage = {
    title: `结果方法`,
    mdFile: './index.md',
    route: "result-methods",
    category: QueryCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ResultMethodsPage;

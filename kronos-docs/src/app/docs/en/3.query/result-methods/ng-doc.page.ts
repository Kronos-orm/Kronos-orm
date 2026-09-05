import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes query terminal methods and result shapes.
 * @status:info NEW
 */
const ResultMethodsPage: NgDocPage = {
    title: `Result Methods`,
    mdFile: './index.md',
    route: "result-methods",
    category: QueryCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ResultMethodsPage;

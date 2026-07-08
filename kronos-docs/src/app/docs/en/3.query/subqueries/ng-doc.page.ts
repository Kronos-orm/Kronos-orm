import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes subqueries, generated projections, INSERT SELECT, CTAS, and derived query sources.
 * @status:info NEW
 */
const SubqueriesPage: NgDocPage = {
    title: `Subqueries`,
    mdFile: './index.md',
    route: "subqueries",
    category: QueryCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SubqueriesPage;

import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍 orderBy、limit、page、withTotal、聚合函数、groupBy 和 having。
 * @status:info NEW
 */
const SortingPaginationAggregationPage: NgDocPage = {
    title: `排序、分页与聚合`,
    mdFile: './index.md',
    route: "sorting-pagination-aggregation",
    category: QueryCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SortingPaginationAggregationPage;

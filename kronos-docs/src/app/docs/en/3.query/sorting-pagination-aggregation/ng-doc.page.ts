import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes orderBy, limit, page, withTotal, aggregate functions, groupBy, and having.
 * @status:info NEW
 */
const SortingPaginationAggregationPage: NgDocPage = {
    title: `Sorting, Pagination, and Aggregation`,
    mdFile: './index.md',
    route: "sorting-pagination-aggregation",
    category: QueryCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SortingPaginationAggregationPage;

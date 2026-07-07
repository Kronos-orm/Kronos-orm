import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何执行批量 SQL 语句。
 */
const BatchOperationsPage: NgDocPage = {
    title: `批量操作`,
    mdFile: './index.md',
    route: "batch-operations",
    category: MutationCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default BatchOperationsPage;

import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to execute batch SQL statements.
 */
const BatchOperationsPage: NgDocPage = {
    title: `Batch Operations`,
    mdFile: './index.md',
    route: "batch-operations",
    category: MutationCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default BatchOperationsPage;

import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to insert records into the database.
 * @status:success UPDATED
 */
const InsertRecordsPage: NgDocPage = {
    title: `Insert`,
    mdFile: './index.md',
    route: "insert",
    category: MutationCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default InsertRecordsPage;

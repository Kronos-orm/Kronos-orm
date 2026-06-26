import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to insert records into the database.
 * @status:success UPDATED
 */
const InsertRecordsPage: NgDocPage = {
    title: `Insert Records`,
    mdFile: './index.md',
    route: "insert-records",
    category: DatabaseCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default InsertRecordsPage;

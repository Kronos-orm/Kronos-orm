import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to upsert records in the database.
 * @status:success UPDATED
 */
const UpsertRecordsPage: NgDocPage = {
    title: `Upsert Records`,
    mdFile: './index.md',
    route: "upsert-records",
    category: DatabaseCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

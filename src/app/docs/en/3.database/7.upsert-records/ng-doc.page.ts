import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to insert or update records into the database.
 * @status:info coming soon
 */
const UpsertRecordsPage: NgDocPage = {
    title: `Update Insertion`,
    mdFile: './index.md',
    route: "upsert-records",
    category: DatabaseCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to upsert records in the database.
 * @status:success UPDATED
 */
const UpsertRecordsPage: NgDocPage = {
    title: `Upsert`,
    mdFile: './index.md',
    route: "upsert",
    category: MutationCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

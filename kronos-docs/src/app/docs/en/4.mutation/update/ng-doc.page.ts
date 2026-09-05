import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to update records in the database.
 * @status:success UPDATED
 */
const UpdateRecordsPage: NgDocPage = {
    title: `Update`,
    mdFile: './index.md',
    route: "update",
    category: MutationCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpdateRecordsPage;

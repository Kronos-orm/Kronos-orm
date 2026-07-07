import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to delete records from the database.
 * @status:success UPDATED
 */
const DeleteRecordsPage: NgDocPage = {
    title: `Delete`,
    mdFile: './index.md',
    route: "delete",
    category: MutationCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DeleteRecordsPage;

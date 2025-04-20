import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to delete records from the database.
 * @status:success UPDATED
 */
const DeleteRecordsPage: NgDocPage = {
    title: `Delete Records`,
    mdFile: './index.md',
    route: "delete-records",
    category: DatabaseCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DeleteRecordsPage;

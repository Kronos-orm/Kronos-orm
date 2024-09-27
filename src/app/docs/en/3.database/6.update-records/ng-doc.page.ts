import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to update database row records.
 * @status:info coming soon
 */
const UpdateRecordsPage: NgDocPage = {
    title: `Update Records`,
    mdFile: './index.md',
    route: "update-records",
    category: DatabaseCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpdateRecordsPage;

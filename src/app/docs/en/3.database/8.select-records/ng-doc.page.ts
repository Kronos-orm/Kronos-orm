import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to select records from the database.
 * @status:success UPDATED
 */
const SelectRecordsPage: NgDocPage = {
    title: `Select Records`,
    mdFile: './index.md',
    route: "select-records",
    category: DatabaseCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectRecordsPage;

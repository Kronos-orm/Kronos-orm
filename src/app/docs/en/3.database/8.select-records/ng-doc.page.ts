import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * English Documentation is not available yet.
 * This chapter describes how to select records from the database.
 * @status:warning WIP
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

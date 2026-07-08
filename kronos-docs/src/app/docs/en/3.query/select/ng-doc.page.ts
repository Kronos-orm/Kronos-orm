import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to select records from the database.
 * @status:success UPDATED
 */
const SelectRecordsPage: NgDocPage = {
    title: `Select`,
    mdFile: './index.md',
    route: "select",
    category: QueryCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectRecordsPage;

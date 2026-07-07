import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to query multiple tables for correlated data.
 * @status:success UPDATED
 */
const SelectJoinTablesPage: NgDocPage = {
    title: `Join`,
    mdFile: './index.md',
    route: "join",
    category: QueryCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectJoinTablesPage;

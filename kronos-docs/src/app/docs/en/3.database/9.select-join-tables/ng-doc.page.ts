import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to query multiple tables for correlated data (perhaps you'd also like to take a look at {{ $.keyword("advanced/reference-select", ["cascade query"]) }}).
 * @status:success UPDATED
 */
const SelectJoinTablesPage: NgDocPage = {
    title: `Select Join Tables`,
    mdFile: './index.md',
    route: "select-join-tables",
    category: DatabaseCategory,
    order: 9,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectJoinTablesPage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * @status:warning PREPARING
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

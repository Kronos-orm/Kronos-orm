import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes CREATE TABLE AS SELECT with Kronos selectable queries.
 * @status:info NEW
 */
const CreateTableAsSelectPage: NgDocPage = {
    title: `Create Table As Select`,
    mdFile: './index.md',
    route: "create-table-as-select",
    category: DatabaseCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CreateTableAsSelectPage;

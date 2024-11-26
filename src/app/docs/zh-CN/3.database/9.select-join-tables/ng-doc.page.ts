import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * @status:success 有更新
 */
const SelectJoinTablesPage: NgDocPage = {
    title: `连表查询`,
    mdFile: './index.md',
    route: "select-join-tables",
    category: DatabaseCategory,
    order: 9,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectJoinTablesPage;

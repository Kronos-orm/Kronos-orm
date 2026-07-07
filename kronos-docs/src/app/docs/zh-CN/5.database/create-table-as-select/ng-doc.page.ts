import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何使用 Kronos 查询创建新表。
 */
const CreateTableAsSelectPage: NgDocPage = {
    title: `基于查询创建表`,
    mdFile: './index.md',
    route: "create-table-as-select",
    category: DatabaseCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CreateTableAsSelectPage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何创建、删除、清空和同步数据库表。
 * @status:info coming soon
 */
const DatabaseOperationPage: NgDocPage = {
    title: `Database Operation`,
    mdFile: './index.md',
    route: "database-operation",
    category: DatabaseCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseOperationPage;

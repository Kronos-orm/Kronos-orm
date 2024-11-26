import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何删除数据库行记录。
 * @status:warning PREPARING
 */
const DeleteRecordsPage: NgDocPage = {
    title: `Delete Records`,
    mdFile: './index.md',
    route: "delete-records",
    category: DatabaseCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DeleteRecordsPage;

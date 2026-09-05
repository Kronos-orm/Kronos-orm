import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何删除数据库行记录。
 */
const DeleteRecordsPage: NgDocPage = {
    title: `删除`,
    mdFile: './index.md',
    route: "delete",
    category: MutationCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DeleteRecordsPage;

import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何更新数据库行记录。
 */
const UpdateRecordsPage: NgDocPage = {
    title: `更新`,
    mdFile: './index.md',
    route: "update",
    category: MutationCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpdateRecordsPage;

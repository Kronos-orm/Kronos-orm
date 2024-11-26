import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何查询数据库中的记录。
 * @status:warning PREPARING
 */
const SelectRecordsPage: NgDocPage = {
    title: `Select Records`,
    mdFile: './index.md',
    route: "select-records",
    category: DatabaseCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectRecordsPage;

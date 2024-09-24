import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何向数据库中插入或更新记录。
 * @status:info 有更新
 */
const UpsertRecordsPage: NgDocPage = {
    title: `更新插入`,
    mdFile: './index.md',
    route: "upsert-records",
    category: DatabaseCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

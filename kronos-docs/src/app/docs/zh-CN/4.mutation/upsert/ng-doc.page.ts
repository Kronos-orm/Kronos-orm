import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何向数据库中插入或更新记录。
 */
const UpsertRecordsPage: NgDocPage = {
    title: `更新插入`,
    mdFile: './index.md',
    route: "upsert",
    category: MutationCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

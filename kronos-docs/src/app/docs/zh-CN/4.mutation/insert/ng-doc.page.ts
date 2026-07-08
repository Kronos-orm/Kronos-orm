import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何向数据库插入记录。
 */
const InsertRecordsPage: NgDocPage = {
    title: `插入`,
    mdFile: './index.md',
    route: "insert",
    category: MutationCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default InsertRecordsPage;

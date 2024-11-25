import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何向数据库插入记录。
 * @status:info coming soon
 */
const InsertRecordsPage: NgDocPage = {
    title: `Insert Records`,
    mdFile: './index.md',
    route: "insert-records",
    category: DatabaseCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default InsertRecordsPage;

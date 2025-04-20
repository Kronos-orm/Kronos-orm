import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何在Kronos中使用数据库事务。
 * @status:info 新
 */
const TransactPage: NgDocPage = {
    title: `数据库事务`,
    mdFile: './index.md',
    route: "transact",
    category: DatabaseCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TransactPage;

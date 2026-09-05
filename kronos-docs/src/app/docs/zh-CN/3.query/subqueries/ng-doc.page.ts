import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍子查询、生成投影、INSERT SELECT、CTAS 和派生查询源。
 * @status:info NEW
 */
const SubqueriesPage: NgDocPage = {
    title: `子查询`,
    mdFile: './index.md',
    route: "subqueries",
    category: QueryCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SubqueriesPage;

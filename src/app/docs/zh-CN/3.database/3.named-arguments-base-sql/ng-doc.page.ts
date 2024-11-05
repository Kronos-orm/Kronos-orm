import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos提供了一个基础的SQL构建器，它允许您使用命名参数来构建SQL操作，本章将介绍如何使用该构建器执行SQL查询等操作。
 * @status:success 新
 */
const NamedArgumentsBaseSqlPage: NgDocPage = {
    title: `Sql语句执行（命名参数）`,
    mdFile: './index.md',
    route: "named-arguments-base-sql",
    category: DatabaseCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NamedArgumentsBaseSqlPage;
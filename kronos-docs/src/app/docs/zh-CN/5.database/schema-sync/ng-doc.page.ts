import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述如何根据 KPojo 元数据同步数据库表结构。
 */
const SchemaSyncPage: NgDocPage = {
    title: `表结构同步`,
    mdFile: './index.md',
    route: "schema-sync",
    category: DatabaseCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SchemaSyncPage;

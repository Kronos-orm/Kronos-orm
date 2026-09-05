import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍 Kronos 代码生成器的使用方法，可根据数据库表结构自动生成实体类。
 * @status:info 新
 */
const CodeGeneratorPage: NgDocPage = {
    title: `代码生成器`,
    mdFile: './index.md',
    route: 'codegen',
    order: 1,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CodeGeneratorPage;

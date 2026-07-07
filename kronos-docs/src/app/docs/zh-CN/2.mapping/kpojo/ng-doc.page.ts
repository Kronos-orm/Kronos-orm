import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何定义一个Kronos表数据实体类。
 * @status:success 有更新
 */
const TableClassDefinition: NgDocPage = {
    title: `KPojo`,
    mdFile: './index.md',
    route: 'kpojo',
    category: MappingCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TableClassDefinition;

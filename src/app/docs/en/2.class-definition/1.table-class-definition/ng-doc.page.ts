import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何定义一个Kronos表数据实体类。
 * @status:info coming soon
 */
const TableClassDefinition: NgDocPage = {
    title: `Table Class Definition`,
    mdFile: './index.md',
    route: 'table-class-definition',
    category: ClassDefinitionCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TableClassDefinition;

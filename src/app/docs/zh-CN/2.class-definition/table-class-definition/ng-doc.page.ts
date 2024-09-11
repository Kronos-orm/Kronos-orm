import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const TableClassDefinition: NgDocPage = {
    title: `数据表类定义`,
    mdFile: './index.md',
    category: ClassDefinitionCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TableClassDefinition;

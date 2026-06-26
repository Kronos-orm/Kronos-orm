import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to define a Kronos table data entity class.
 * @status:info NEW
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

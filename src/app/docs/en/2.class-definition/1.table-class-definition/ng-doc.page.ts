import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to define a Kronos table data entity class.
 * @status:info coming soon
 */
const TableClassDefinition: NgDocPage = {
    title: `Data Table Class Definition`,
    mdFile: './index.md',
    route: 'table-class-definition',
    category: ClassDefinitionCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TableClassDefinition;

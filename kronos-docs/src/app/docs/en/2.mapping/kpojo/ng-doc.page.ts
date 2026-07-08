import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to define a Kronos table data entity class.
 * @status:info NEW
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

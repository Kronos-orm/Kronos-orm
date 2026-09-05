import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter introduces the Kronos code generator, which automatically generates entity classes from database tables.
 * @status:info NEW
 */
const CodeGeneratorPage: NgDocPage = {
    title: `Code Generator`,
    mdFile: './index.md',
    route: 'codegen',
    order: 1,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CodeGeneratorPage;

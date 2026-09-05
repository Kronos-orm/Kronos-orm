import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter explains how to install and use the Kronos ORM AI skill in coding assistants.
 * @status:info NEW
 */
const UsingKronosWithAiPage: NgDocPage = {
    title: `Using Kronos with AI`,
    mdFile: './index.md',
    route: 'using-kronos-with-ai',
    category: ResourcesCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UsingKronosWithAiPage;

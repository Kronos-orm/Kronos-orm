import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter introduces the Kronos IntelliJ IDEA plugin setup and editor diagnostics.
 * @status:info NEW
 */
const IdeaPluginPage: NgDocPage = {
    title: `IntelliJ IDEA Plugin`,
    mdFile: './index.md',
    route: 'idea-plugin',
    order: 2,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default IdeaPluginPage;

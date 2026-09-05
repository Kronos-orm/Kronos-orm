import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter collects common setup, query, and runtime troubleshooting steps.
 * @status:info NEW
 */
const TroubleshootingPage: NgDocPage = {
    title: `Troubleshooting`,
    mdFile: './index.md',
    route: 'troubleshooting',
    order: 8,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TroubleshootingPage;

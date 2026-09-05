import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter explains how contributors can use the Kronos developer AI skill while working on the repository.
 * @status:info NEW
 */
const ContributingWithAiPage: NgDocPage = {
    title: `Contributing with AI`,
    mdFile: './index.md',
    route: 'contributing-with-ai',
    category: ResourcesCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ContributingWithAiPage;

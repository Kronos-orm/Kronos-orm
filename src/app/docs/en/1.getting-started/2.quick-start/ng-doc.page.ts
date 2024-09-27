import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to get started with Kronos quickly, and you can find some sample projects [here](https://github.com/Kronos-orm?tab=repositories).
 * @status:success new
 */
const QuickStartPage: NgDocPage = {
    title: `Quick Start`,
    mdFile: './index.md',
    route: "quick-start",
    category: GettingStartedCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent},
};

export default QuickStartPage;

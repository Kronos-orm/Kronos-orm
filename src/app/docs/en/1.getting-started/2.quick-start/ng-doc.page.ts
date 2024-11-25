import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何快速开始使用Kronos，你可以在[这里](https://github.com/Kronos-orm?tab=repositories)找到一些示例项目。
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

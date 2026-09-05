import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍贡献者如何在开发 Kronos 仓库时使用开发者 AI 技能。
 * @status:info 新
 */
const ContributingWithAiPage: NgDocPage = {
    title: `使用 AI 参与开发`,
    mdFile: './index.md',
    route: 'contributing-with-ai',
    category: ResourcesCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ContributingWithAiPage;

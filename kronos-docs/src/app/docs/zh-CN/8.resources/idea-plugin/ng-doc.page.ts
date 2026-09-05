import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍 Kronos IntelliJ IDEA 插件配置和编辑器诊断。
 * @status:info 新
 */
const IdeaPluginPage: NgDocPage = {
    title: `IntelliJ IDEA 插件`,
    mdFile: './index.md',
    route: 'idea-plugin',
    order: 2,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default IdeaPluginPage;

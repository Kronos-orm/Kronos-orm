import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos支持创建或引入语言包插件定义Kronos内置的文字提示。
 * @status:warning WIP
 */
const LanguagePage: NgDocPage = {
    title: `语言包`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 4,
    route: 'language',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LanguagePage;

import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Data Guard 插件是一个Kronos插件，提供数据保护功能。
 * @status:info 新
 */
const DataGuardPluginPage: NgDocPage = {
    title: `数据保护插件`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 6,
    route: 'data-guard',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DataGuardPluginPage;

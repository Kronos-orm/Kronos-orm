import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Data Guard 插件根据用户配置的策略拒绝危险写入和表操作。
 * @status:info 新
 */
const DataGuardPluginPage: NgDocPage = {
    title: `数据保护插件`,
    mdFile: './index.md',
    category: AdvancedCategory,
    order: 13,
    route: 'data-guard',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DataGuardPluginPage;

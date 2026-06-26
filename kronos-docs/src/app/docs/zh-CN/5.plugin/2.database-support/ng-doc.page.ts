import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos支持创建或引入插件增加更多数据库类型支持。
 * @status:info 新
 */
const DatabaseSupportPage: NgDocPage = {
    title: `数据库支持扩展`,
    mdFile: './index.md',
    route: 'database-support',
    order: 2,
    category: PluginCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseSupportPage;

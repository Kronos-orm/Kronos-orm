import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";

/**
 * Kronos支持创建或引入插件增加更多数据库类型支持。
 * @status:info coming soon
 */
const DatabaseSupportPage: NgDocPage = {
	title: `Database Support Extension`,
	mdFile: './index.md',
	route: 'database-support',
  category: PluginCategory
};

export default DatabaseSupportPage;

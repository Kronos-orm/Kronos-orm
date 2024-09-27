import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";

/**
 * Kronos supports creating or importing plugins to add more database type support.
 * @status:info coming soon
 */
const DatabaseSupportPage: NgDocPage = {
	title: `Database Support Extension`,
	mdFile: './index.md',
	route: 'database-support',
  category: PluginCategory
};

export default DatabaseSupportPage;

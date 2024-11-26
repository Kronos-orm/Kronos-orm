import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos supports the creation or introduction of plugins to add more database type support.
 * @status:info NEW
 */
const DatabaseSupportPage: NgDocPage = {
    title: `Database Support Extension`,
    mdFile: './index.md',
    route: 'database-support',
    order: 2,
    category: PluginCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseSupportPage;

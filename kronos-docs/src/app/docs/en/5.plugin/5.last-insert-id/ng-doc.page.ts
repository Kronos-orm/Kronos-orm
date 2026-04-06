import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * LastInsertId is a Kronos plugin that retrieves the last inserted ID.
 * @status:warning WIP
 */
const LastInsertIdPluginPage: NgDocPage = {
    title: `Last Insert Id`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 5,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPluginPage;

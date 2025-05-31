import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * LastInsertId是一个Kronos插件，用于获取最后插入的ID。
 * @status:warning WIP
 */
const LastInsertIdPluginPage: NgDocPage = {
    title: `获取最后插入的ID`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 5,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPluginPage;

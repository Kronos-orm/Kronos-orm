import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * LastInsertId是一个Kronos插件，用于获取最后插入的ID。
 * @status:info 新
 */
const LastInsertIdPluginPage: NgDocPage = {
    title: `获取最后插入的 ID`,
    mdFile: './index.md',
    category: MutationCategory,
    order: 6,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPluginPage;

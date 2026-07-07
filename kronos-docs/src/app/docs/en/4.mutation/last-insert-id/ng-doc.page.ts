import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * LastInsertId is a Kronos plugin that retrieves the last inserted ID.
 * @status:info NEW
 */
const LastInsertIdPluginPage: NgDocPage = {
    title: `Last Insert ID`,
    mdFile: './index.md',
    category: MutationCategory,
    order: 6,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPluginPage;

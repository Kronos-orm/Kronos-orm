import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Reads database-generated identity IDs from insert results.
 * @status:info NEW
 */
const LastInsertIdPage: NgDocPage = {
    title: `Last Insert ID`,
    mdFile: './index.md',
    category: MutationCategory,
    order: 6,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPage;

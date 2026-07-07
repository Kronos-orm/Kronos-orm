import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes optimistic lock version fields in mutation APIs.
 */
const OptimisticLockPage: NgDocPage = {
    title: `Optimistic Lock`,
    mdFile: './index.md',
    route: "optimistic-lock",
    category: MutationCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default OptimisticLockPage;

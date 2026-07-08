import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述 mutation API 中的乐观锁版本字段。
 */
const OptimisticLockPage: NgDocPage = {
    title: `乐观锁`,
    mdFile: './index.md',
    route: "optimistic-lock",
    category: MutationCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default OptimisticLockPage;

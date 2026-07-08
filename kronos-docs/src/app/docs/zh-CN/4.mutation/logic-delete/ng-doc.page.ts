import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章描述 mutation API 中的逻辑删除行为。
 */
const LogicDeletePage: NgDocPage = {
    title: `逻辑删除`,
    mdFile: './index.md',
    route: "logic-delete",
    category: MutationCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LogicDeletePage;

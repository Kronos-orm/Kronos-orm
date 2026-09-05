import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes logical delete behavior in mutation APIs.
 */
const LogicDeletePage: NgDocPage = {
    title: `Logic Delete`,
    mdFile: './index.md',
    route: "logic-delete",
    category: MutationCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LogicDeletePage;

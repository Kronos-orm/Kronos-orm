import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to configure a cascade deletion action in Kronos.
 * @status:stable
 */
const CascadeDeleteActionPage: NgDocPage = {
    title: `Cascading Deletion Action`,
    mdFile: './index.md',
    route: 'cascade-delete-action',
    order: 7,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeleteActionPage;

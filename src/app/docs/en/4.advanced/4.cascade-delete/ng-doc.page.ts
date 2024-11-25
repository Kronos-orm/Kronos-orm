import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联删除功能。
 * @status:info coming soon
 */
const CascadeDeletePage: NgDocPage = {
    title: `Cascade Delete`,
    mdFile: './index.md',
    route: 'cascade-delete',
    category: AdvancedCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeletePage;

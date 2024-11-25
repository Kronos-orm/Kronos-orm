import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联删除功能。
 * @status:success 新
 */
const CascadeDeletePage: NgDocPage = {
    title: `级联删除`,
    mdFile: './index.md',
    route: 'cascade-delete',
    category: AdvancedCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeletePage;

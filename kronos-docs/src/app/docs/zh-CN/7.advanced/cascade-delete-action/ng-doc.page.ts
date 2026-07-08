import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何配置级联删除策略。
 * @status:stable
 */
const CascadeDeleteActionPage: NgDocPage = {
    title: `级联删除策略`,
    mdFile: './index.md',
    route: 'cascade-delete-action',
    order: 7,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeleteActionPage;

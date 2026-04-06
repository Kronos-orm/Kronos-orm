import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联更新功能。
 * @status:info 新
 */
const CascadeUpdatePage: NgDocPage = {
    title: `级联更新`,
    mdFile: './index.md',
    route: 'cascade-update',
    category: AdvancedCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpdatePage;

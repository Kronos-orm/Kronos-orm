import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联插入功能。
 * @status:info coming soon
 */
const CascadeInsertPage: NgDocPage = {
    title: `Cascade Insert`,
    mdFile: './index.md',
    route: 'cascade-insert',
    category: AdvancedCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeInsertPage;

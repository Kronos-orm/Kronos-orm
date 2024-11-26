import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联插入功能。
 * @status:info 新
 */
const CascadeInsertPage: NgDocPage = {
    title: `级联插入`,
    mdFile: './index.md',
    route: 'cascade-insert',
    category: AdvancedCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeInsertPage;

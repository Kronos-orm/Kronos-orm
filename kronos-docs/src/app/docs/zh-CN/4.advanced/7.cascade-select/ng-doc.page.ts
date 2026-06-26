import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联查询功能。
 * @status:info 新
 */
const CascadeSelectPage: NgDocPage = {
    title: `级联查询`,
    mdFile: './index.md',
    route: 'cascade-select',
    category: AdvancedCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeSelectPage;

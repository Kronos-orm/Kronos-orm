import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联插入或更新功能。
 * @status:success 新
 */
const CascadeUpsertPage: NgDocPage = {
    title: `级联更新插入`,
    mdFile: './index.md',
    route: 'cascade-upsert',
    category: AdvancedCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpsertPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的级联插入或更新功能。
 * @status:info coming soon
 */
const CascadeUpsertPage: NgDocPage = {
    title: `Cascade Upsert`,
    mdFile: './index.md',
    route: 'cascade-upsert',
    category: AdvancedCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpsertPage;

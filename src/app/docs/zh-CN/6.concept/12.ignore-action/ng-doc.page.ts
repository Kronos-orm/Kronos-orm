import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * 本文将指导您如何配置忽略策略。
 * @status:success 新
 */
const IgnoreActionPage: NgDocPage = {
    title: `查询忽略策略`,
    mdFile: './index.md',
    route: 'ignore-action',
    order: 12,
    category: ConceptCategory
};

export default IgnoreActionPage;

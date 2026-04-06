import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * 本文将指导您如何配置级联删除策略。
 * @status:stable
 */
const CascadeDeleteActionPage: NgDocPage = {
    title: `级联删除策略`,
    mdFile: './index.md',
    route: 'cascade-delete-action',
    order: 3,
    category: ConceptCategory
};

export default CascadeDeleteActionPage;

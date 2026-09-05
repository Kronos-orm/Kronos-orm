import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";

/**
 * 本文将指导您如何配置忽略策略。
 * @status:info 新
 */
const IgnoreActionPage: NgDocPage = {
    title: `忽略策略`,
    mdFile: './index.md',
    route: 'ignore',
    order: 9,
    category: MappingCategory
};

export default IgnoreActionPage;

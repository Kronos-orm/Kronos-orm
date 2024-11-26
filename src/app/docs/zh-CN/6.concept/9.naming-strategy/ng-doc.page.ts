import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * `KronosNamingStrategy`是一个接口，用于定义表名和列名的转换策略。
 * @status:info 有更新
 */
const NamingStrategyPage: NgDocPage = {
    title: `命名策略`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 9,
    route: 'naming-strategy'
};

export default NamingStrategyPage;

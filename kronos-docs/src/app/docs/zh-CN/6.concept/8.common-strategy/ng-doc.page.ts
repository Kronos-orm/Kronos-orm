import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * `KronosCommonStrategy`是更新时间/创建时间/逻辑删除配置策略的通用配置策略接口。
 * @status:success 有更新
 */
const CommonStrategyPage: NgDocPage = {
    title: `通用策略`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 8,
    route: 'common-strategy'
};

export default CommonStrategyPage;

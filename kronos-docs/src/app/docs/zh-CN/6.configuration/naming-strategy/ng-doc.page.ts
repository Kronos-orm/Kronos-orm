import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * `KronosNamingStrategy`是一个接口，用于定义表名和列名的转换策略。
 * @status:success 有更新
 */
const NamingStrategyPage: NgDocPage = {
    title: `命名策略`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 4,
    route: 'naming-strategy'
};

export default NamingStrategyPage;

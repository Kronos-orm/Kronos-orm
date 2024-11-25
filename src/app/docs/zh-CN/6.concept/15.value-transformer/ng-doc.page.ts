import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * 本章将介绍 值转换器 的使用方法以及如何自定义值转换器。
 * @status:success 新
 */
const ValueTransformerPage: NgDocPage = {
    title: `值转换器`,
    mdFile: './index.md',
    route: 'value-transformer',
    category: ConceptCategory,
    order: 15
};

export default ValueTransformerPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的内置函数功能及自定义函数功能。
 * @status:info coming soon
 */
const BuiltInFunctionPage: NgDocPage = {
    title: `Built-in Function`,
    mdFile: './index.md',
    route: 'built-in-function',
    category: AdvancedCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default BuiltInFunctionPage;

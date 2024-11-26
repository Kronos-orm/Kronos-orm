import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的内置函数功能及自定义函数功能。
 * @status:info 新
 */
const CustomFunctionPage: NgDocPage = {
    title: `自定义函数与方言`,
    mdFile: './index.md',
    route: 'custom-function',
    category: AdvancedCategory,
    order: 9,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CustomFunctionPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use Kronos' built-in functions and custom functions.
 * @status:success UPDATED
 */
const CustomFunctionPage: NgDocPage = {
    title: `Custom Function and Dialect`,
    mdFile: './index.md',
    route: 'custom-function',
    category: AdvancedCategory,
    order: 9,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CustomFunctionPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use Kronos' built-in functions and custom functions.
 * @status:info updated recently
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

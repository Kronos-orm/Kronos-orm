import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use Kronos' built-in functions and custom functions.
 * @status:success UPDATED
 */
const BuiltInFunctionPage: NgDocPage = {
    title: `Functions`,
    mdFile: './index.md',
    route: "functions",
    category: QueryCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default BuiltInFunctionPage;

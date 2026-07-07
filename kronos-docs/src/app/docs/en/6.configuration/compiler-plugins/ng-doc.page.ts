import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Configure Kronos compiler plugins and read user-visible DSL diagnostics.
 * @status:info NEW
 */
const LanguagePage: NgDocPage = {
    title: `Compiler Plugins`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 8,
    route: 'compiler-plugins',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LanguagePage;

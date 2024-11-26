import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos supports the creation or introduction of language pack plugins to define Kronos' built-in text prompts.
 * @status:warning PREPARING
 */
const LanguagePage: NgDocPage = {
    title: `Language Plugin`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 3,
    route: 'language',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LanguagePage;

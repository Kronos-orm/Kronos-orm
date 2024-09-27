import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";

/**
 * Kronos supports the creation or introduction of language pack plugins to define Kronos' built-in text prompts.
 * @status:info coming soon
 */
const LanguagePage: NgDocPage = {
	title: `Language pack plugin`,
	mdFile: './index.md',
  category: PluginCategory
};

export default LanguagePage;

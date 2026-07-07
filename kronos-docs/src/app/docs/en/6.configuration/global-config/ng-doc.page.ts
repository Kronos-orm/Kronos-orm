import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to set up Kronos globally.
 * @status:success UPDATED
 */
const GlobalConfigPage: NgDocPage = {
	title: `Global Config`,
	mdFile: './index.md',
	route: "global-config",
	category: ConfigurationCategory,
	order: 1,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default GlobalConfigPage;

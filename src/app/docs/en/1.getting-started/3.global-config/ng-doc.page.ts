import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to set up Kronos globally.
 * @status:success new
 */
const GlobalConfigPage: NgDocPage = {
	title: `Global configuration`,
	mdFile: './index.md',
	route: "global-config",
	category: GettingStartedCategory,
	order: 3,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default GlobalConfigPage;

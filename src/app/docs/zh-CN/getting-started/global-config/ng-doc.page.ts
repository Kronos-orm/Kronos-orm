import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const GlobalConfigPage: NgDocPage = {
	title: `全局设置`,
	mdFile: './index.md',
	category: GettingStartedCategory,
	order: 3,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default GlobalConfigPage;

import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何全局设置Kronos。
 * @status:success 有更新
 */
const GlobalConfigPage: NgDocPage = {
	title: `全局设置`,
	mdFile: './index.md',
	route: "global-config",
	category: ConfigurationCategory,
	order: 1,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default GlobalConfigPage;

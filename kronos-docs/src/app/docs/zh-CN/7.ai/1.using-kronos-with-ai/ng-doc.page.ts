import {NgDocPage} from '@ng-doc/core';
import AiCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍如何在 AI 编程助手中安装和使用 Kronos ORM 技能。
 * @status:info 新的
 */
const UsingKronosWithAiPage: NgDocPage = {
	title: `使用 AI 编写 Kronos 代码`,
	mdFile: './index.md',
	route: "using-kronos-with-ai",
	category: AiCategory,
	order: 1,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default UsingKronosWithAiPage;

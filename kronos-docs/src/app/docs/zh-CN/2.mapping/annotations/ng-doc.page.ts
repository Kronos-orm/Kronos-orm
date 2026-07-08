import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何配置注解。
 * @status:success 有更新
 */
const AnnotationConfigPage: NgDocPage = {
	title: `注解`,
	mdFile: './index.md',
	route: 'annotations',
	category: MappingCategory,
	order: 3,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

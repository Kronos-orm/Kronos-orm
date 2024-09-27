import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何配置注解。
 * @status:success 新注解
 */
const AnnotationConfigPage: NgDocPage = {
	title: `注解配置`,
	mdFile: './index.md',
	route: 'annotation-config',
	category: ClassDefinitionCategory,
	order: 2,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

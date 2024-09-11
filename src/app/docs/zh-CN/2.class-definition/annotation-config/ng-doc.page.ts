import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const AnnotationConfigPage: NgDocPage = {
	title: `注解配置`,
	mdFile: './index.md',
	category: ClassDefinitionCategory,
	order: 2,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

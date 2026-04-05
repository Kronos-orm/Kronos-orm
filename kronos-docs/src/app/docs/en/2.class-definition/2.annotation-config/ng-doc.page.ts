import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to configure annotations.
 * @status:info NEW
 */
const AnnotationConfigPage: NgDocPage = {
	title: `Annotation Config`,
	mdFile: './index.md',
	route: 'annotation-config',
	category: ClassDefinitionCategory,
	order: 2,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

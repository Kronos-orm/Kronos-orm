import {NgDocPage} from '@ng-doc/core';
import ClassDefinitionCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to configure annotations.
 * @status:info coming soon
 */
const AnnotationConfigPage: NgDocPage = {
	title: `Annotation configuration`,
	mdFile: './index.md',
	route: 'annotation-config',
	category: ClassDefinitionCategory,
	order: 2,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

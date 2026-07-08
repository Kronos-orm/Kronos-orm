import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to configure annotations.
 * @status:info NEW
 */
const AnnotationConfigPage: NgDocPage = {
	title: `Annotations`,
	mdFile: './index.md',
	route: 'annotations',
	category: MappingCategory,
	order: 3,
	imports: [AnimateLogoComponent],
	demos: {AnimateLogoComponent}
};

export default AnnotationConfigPage;

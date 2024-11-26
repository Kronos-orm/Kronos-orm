import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will describe how to define cascade relationships in KPojo entity classes.
 * @status:info updated recently
 */
const CascadeDefinitionPage: NgDocPage = {
    title: `Cascade Definition`,
    mdFile: './index.md',
    route: 'cascade-definition',
    category: AdvancedCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDefinitionPage;

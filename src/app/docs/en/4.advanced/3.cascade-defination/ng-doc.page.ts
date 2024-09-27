import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to define cascading relationships in KPojo entity classes.
 * @status:info coming soon
 */
const CascadeDefinitionPage: NgDocPage = {
    title: `Definition of cascade relationships`,
    mdFile: './index.md',
    route: 'cascade-definition',
    category: AdvancedCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDefinitionPage;

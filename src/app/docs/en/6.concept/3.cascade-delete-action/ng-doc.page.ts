import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * This article will guide you on how to configure a cascade deletion policy.
 * @status:stable
 */
const CascadeDeleteActionPage: NgDocPage = {
    title: `Cascading Deletion Action`,
    mdFile: './index.md',
    route: 'cascade-delete-action',
    order: 3,
    category: ConceptCategory
};

export default CascadeDeleteActionPage;

import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * This chapter will guide you on how to configure a cascade deletion action in Kronos.
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

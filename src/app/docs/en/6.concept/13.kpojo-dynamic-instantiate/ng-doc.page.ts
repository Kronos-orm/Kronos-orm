import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * This chapter will show you how Kronos enables dynamic conversion of KClass to KPojo instances with no reflection and zero overhead.
 * @status:success new
 */
const KPojoGenericInstantiatePage: NgDocPage = {
    title: `KPojo Dynamic Instantiate`,
    mdFile: './index.md',
    route: 'kpojo-dynamic-instantiate',
    order: 13,
    category: ConceptCategory
};

export default KPojoGenericInstantiatePage;

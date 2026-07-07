import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will show you how Kronos enables dynamic conversion of KClass to KPojo instances with no reflection and zero overhead.
 * @status:info NEW
 */
const KPojoGenericInstantiatePage: NgDocPage = {
    title: `KPojo Dynamic Instantiate`,
    mdFile: './index.md',
    route: 'kpojo-dynamic-instantiate',
    order: 14,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KPojoGenericInstantiatePage;

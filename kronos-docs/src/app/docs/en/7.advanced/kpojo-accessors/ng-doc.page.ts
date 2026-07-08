import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes the property dynamic accessor provided by Kronos for KPojo at compile time, which supports dynamically accessing or modifying property values based on the property name at runtime, and does not rely on reflection for higher performance, and is recommended.
 * @status:info NEW
 */
const KPojoPropAccessor: NgDocPage = {
    title: `KPojo Accessors`,
    mdFile: './index.md',
    route: "kpojo-accessors",
    order: 10,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KPojoPropAccessor;

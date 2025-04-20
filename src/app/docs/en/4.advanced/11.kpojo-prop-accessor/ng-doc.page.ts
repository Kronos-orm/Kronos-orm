import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes the property dynamic accessor provided by Kronos for KPojo in the compiler, which supports dynamically accessing or modifying property values based on the property name at runtime, and does not rely on reflection for higher performance, and is recommended.
 * @status:info NEW
 */
const KPojoPropAccessor: NgDocPage = {
    title: `Compile-time Generated KPojo Property getter/setter`,
    mdFile: './index.md',
    route: "kpojo-prop-accessor",
    order: 11,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KPojoPropAccessor;

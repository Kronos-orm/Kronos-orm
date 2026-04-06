import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍Kronos在编译期为KPojo生成的属性动态存取器，支持在运行时动态地根据属性名来访问或修改属性值，且不依赖反射，性能更高，推荐使用。
 * @status:info 新
 */
const KPojoPropAccessor: NgDocPage = {
    title: `KPojo属性动态存取器`,
    mdFile: './index.md',
    route: "kpojo-prop-accessor",
    order: 11,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KPojoPropAccessor;

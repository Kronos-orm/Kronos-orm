import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文介绍 Kronos 如何根据完整 KType 元数据创建 KPojo 实例，而不使用反射构造。
 * @status:info 新
 */
const KPojoGenericInstantiatePage: NgDocPage = {
    title: `KPojo的动态实例化`,
    mdFile: './index.md',
    route: 'kpojo-dynamic-instantiate',
    order: 14,
    category: AdvancedCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KPojoGenericInstantiatePage;

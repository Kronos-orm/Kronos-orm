import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * 本文将为您介绍Kronos如何实现动态将KClass转换为KPojo实例，且不需要反射，零开销。
 * @status:success 新
 */
const KPojoGenericInstantiatePage: NgDocPage = {
    title: `KPojo的泛型实例化`,
    mdFile: './index.md',
    route: 'kpojo-generic-instantiate',
    order: 13,
    category: ConceptCategory
};

export default KPojoGenericInstantiatePage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos通过自定义创建继承`KronosDataSourceWrapper`接口的包装类，可以轻松与第三方框架结合使用。
 * @status:info 新
 */
const DatasourceWrapperAndThirdPartFrameworkPage: NgDocPage = {
    title: `数据源及三方框架扩展`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 8,
    route: 'custom-wrapper',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatasourceWrapperAndThirdPartFrameworkPage;

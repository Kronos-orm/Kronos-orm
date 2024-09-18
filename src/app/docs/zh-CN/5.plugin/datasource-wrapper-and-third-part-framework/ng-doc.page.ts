import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const DatasourceWrapperAndThirdPartFrameworkPage: NgDocPage = {
    title: `数据源及三方框架扩展`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 0,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatasourceWrapperAndThirdPartFrameworkPage;

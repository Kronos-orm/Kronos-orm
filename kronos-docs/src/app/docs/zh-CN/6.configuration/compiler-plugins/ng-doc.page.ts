import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 配置 Kronos 编译器插件，并读取用户可见的 DSL 诊断。
 * @status:info 新
 */
const LanguagePage: NgDocPage = {
    title: `编译器插件`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 8,
    route: 'compiler-plugins',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LanguagePage;

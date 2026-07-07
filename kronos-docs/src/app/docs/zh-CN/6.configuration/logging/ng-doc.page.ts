import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 配置 Kronos SQL 执行日志，使用内置日志或 kronos-logging 适配器。
 * @status:info 新
 */
const LoggingPage: NgDocPage = {
    title: `Kronos-logging`,
    mdFile: './index.md',
    route: 'logging',
    order: 7,
    category: ConfigurationCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LoggingPage;

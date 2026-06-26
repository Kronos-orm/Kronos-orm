import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos-logging 插件是一个用于记录日志的插件，支持多种日志级别和输出方式，并且支持对接到Slf4j、Apache Commons Logging、Android Logging等日志框架。
 * @status:info 新
 */
const LoggingPage: NgDocPage = {
    title: `Kronos-logging`,
    mdFile: './index.md',
    route: 'logging',
    order: 4,
    category: PluginCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LoggingPage;

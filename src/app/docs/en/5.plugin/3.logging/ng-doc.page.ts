import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos-logging plugin is a plugin for logging , support for a variety of logging levels and output methods , and support for logging frameworks such as Slf4j, Apache Commons Logging, Android Logging and so on.
 * @status:warning WIP
 */
const LoggingPage: NgDocPage = {
    title: `Kronos-logging`,
    mdFile: './index.md',
    route: 'logging',
    order: 3,
    category: PluginCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LoggingPage;

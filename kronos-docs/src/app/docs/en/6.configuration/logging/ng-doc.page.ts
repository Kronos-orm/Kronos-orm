import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Configure Kronos SQL execution logs with the bundled logger or a kronos-logging adapter.
 * @status:info NEW
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

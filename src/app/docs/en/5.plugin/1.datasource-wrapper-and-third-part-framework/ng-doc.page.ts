import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos can be easily used with third-party frameworks by customizing the creation of wrapper classes that inherit the `KronosDataSourceWrapper` interface.
 * @status:info coming soon
 */
const DatasourceWrapperAndThirdPartFrameworkPage: NgDocPage = {
    title: `Data Source and Third-Party Framework Expansion`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 0,
    route: 'datasource-wrapper-and-third-part-framework',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatasourceWrapperAndThirdPartFrameworkPage;

import {NgDocPage} from '@ng-doc/core';
import PluginCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Data Guard Plugin is a Kronos plugin that provides data protection features.
 * @status:warning WIP
 */
const DataGuardPluginPage: NgDocPage = {
    title: `Data Guard Plugin`,
    mdFile: './index.md',
    category: PluginCategory,
    order: 6,
    route: 'data-guard',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DataGuardPluginPage;

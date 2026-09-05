import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Data Guard Plugin rejects dangerous write and table operations according to user-defined policies.
 * @status:info NEW
 */
const DataGuardPluginPage: NgDocPage = {
    title: `Data Guard Plugin`,
    mdFile: './index.md',
    category: AdvancedCategory,
    order: 13,
    route: 'data-guard',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DataGuardPluginPage;

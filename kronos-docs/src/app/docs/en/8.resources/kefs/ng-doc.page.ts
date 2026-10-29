import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter introduces the KEFS setup, Kotlin editor completion and compiler diagnostics.
 * @status:info NEW
 */
const KefsPage: NgDocPage = {
    title: `IDE Support with KEFS`,
    mdFile: './index.md',
    route: 'kefs',
    order: 2,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KefsPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the Cascade Insert or Update feature of Kronos.
 * @status:info coming soon
 */
const CascadeUpsertPage: NgDocPage = {
    title: `Cascade Update Insertion`,
    mdFile: './index.md',
    route: 'cascade-upsert',
    category: AdvancedCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpsertPage;

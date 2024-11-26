import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the cascading insert or update feature of Kronos.
 * @status:success UPDATED
 */
const CascadeUpsertPage: NgDocPage = {
    title: `Cascade Upsert`,
    mdFile: './index.md',
    route: 'cascade-upsert',
    category: AdvancedCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpsertPage;

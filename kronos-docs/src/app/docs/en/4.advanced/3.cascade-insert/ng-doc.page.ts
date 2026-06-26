import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the cascade insert feature of Kronos.
 * @status:success UPDATED
 */
const CascadeInsertPage: NgDocPage = {
    title: `Cascade Insert`,
    mdFile: './index.md',
    route: 'cascade-insert',
    category: AdvancedCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeInsertPage;

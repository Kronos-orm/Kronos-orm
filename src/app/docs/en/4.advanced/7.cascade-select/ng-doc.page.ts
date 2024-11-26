import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the cascade query feature of Kronos.
 * @status:info updated recently
 */
const CascadeSelectPage: NgDocPage = {
    title: `Cascade Select`,
    mdFile: './index.md',
    route: 'cascade-select',
    category: AdvancedCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeSelectPage;

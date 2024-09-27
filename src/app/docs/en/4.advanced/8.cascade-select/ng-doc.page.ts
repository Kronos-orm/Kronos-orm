import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the cascading query feature of Kronos.
 * @status:info coming soon
 */
const CascadeSelectPage: NgDocPage = {
    title: `Cascade Select`,
    mdFile: './index.md',
    route: 'cascade-select',
    category: AdvancedCategory,
    order: 8,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeSelectPage;

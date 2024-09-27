import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use Kronos' cascading update feature.
 * @status:info coming soon
 */
const CascadeUpdatePage: NgDocPage = {
    title: `Cascade Update`,
    mdFile: './index.md',
    route: 'cascade-update',
    category: AdvancedCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeUpdatePage;

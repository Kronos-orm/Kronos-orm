import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use Kronos' Cascade Delete feature.
 * @status:info coming soon
 */
const CascadeDeletePage: NgDocPage = {
    title: `Cascade Delete`,
    mdFile: './index.md',
    route: 'cascade-delete',
    category: AdvancedCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeletePage;
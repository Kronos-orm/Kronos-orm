import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何在KPojo实体类中定义级联关系。
 * @status:info coming soon
 */
const CascadeDefinitionPage: NgDocPage = {
    title: `Cascade Definition`,
    mdFile: './index.md',
    route: 'cascade-definition',
    category: AdvancedCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDefinitionPage;

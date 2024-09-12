import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const ReferenceDefinitionPage: NgDocPage = {
    title: `级联关系定义`,
    mdFile: './index.md',
    category: AdvancedCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ReferenceDefinitionPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const CascadeDeletePage: NgDocPage = {
    title: `级联删除`,
    mdFile: './index.md',
    route: 'cascade-delete',
    category: AdvancedCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeDeletePage;

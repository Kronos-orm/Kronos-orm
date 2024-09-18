import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const CascadeInsertPage: NgDocPage = {
    title: `级联插入`,
    mdFile: './index.md',
    route: 'cascade-insert',
    category: AdvancedCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CascadeInsertPage;

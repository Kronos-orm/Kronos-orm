import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const CascadeDeleteActionPage: NgDocPage = {
    title: `级联删除策略`,
    mdFile: './index.md',
    route: 'cascade-delete-action',
    order: 1,
    category: ConceptCategory
};

export default CascadeDeleteActionPage;

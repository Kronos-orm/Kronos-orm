import {NgDocPage} from '@ng-doc/core';
import MappingCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const CodeFirstPage: NgDocPage = {
    title: `Code First`,
    mdFile: './index.md',
    route: 'code-first',
    order: 11,
    category: MappingCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent},
};

export default CodeFirstPage;

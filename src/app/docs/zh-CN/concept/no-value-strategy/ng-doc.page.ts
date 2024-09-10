import {NgDocPage} from '@ng-doc/core';
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import ConceptCategory from "../ng-doc.category";

const NoValueStrategyPage: NgDocPage = {
    title: `无值策略`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NoValueStrategyPage;

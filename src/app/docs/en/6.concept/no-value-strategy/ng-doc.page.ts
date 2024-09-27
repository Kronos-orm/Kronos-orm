import {NgDocPage} from '@ng-doc/core';
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import ConceptCategory from "../ng-doc.category";

/**
 * @status:info coming soon
 */
const NoValueStrategyPage: NgDocPage = {
    title: `No-value Strategy`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NoValueStrategyPage;

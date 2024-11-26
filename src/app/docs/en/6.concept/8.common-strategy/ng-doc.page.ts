import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * `KronosCommonStrategy` is the common configuration strategy interface for update time/create time/logical delete configuration strategies.
 * @status:info updated recently
 */
const CommonStrategyPage: NgDocPage = {
    title: `Common Strategy`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 8,
    route: 'common-strategy'
};

export default CommonStrategyPage;

import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * `KronosCommonStrategy` is the common configuration strategy interface for update time/create time/logical delete configuration strategies.
 * @status:success UPDATED
 */
const CommonStrategyPage: NgDocPage = {
    title: `Common Strategy`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 3,
    route: 'common-strategy'
};

export default CommonStrategyPage;

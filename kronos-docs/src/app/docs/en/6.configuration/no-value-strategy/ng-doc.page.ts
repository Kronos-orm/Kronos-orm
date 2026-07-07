import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * @status:stable
 */
const NoValueBehaviorPage: NgDocPage = {
    title: `No-value Behavior`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 5,
    route: 'no-value-strategy',
};

export default NoValueBehaviorPage;

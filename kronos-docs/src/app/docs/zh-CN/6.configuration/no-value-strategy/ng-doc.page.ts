import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

const NoValueBehaviorPage: NgDocPage = {
    title: `无值处理`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 5,
    route: 'no-value-strategy',
};

export default NoValueBehaviorPage;

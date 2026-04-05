import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * `KronosNamingStrategy` is an interface used to define the conversion strategy for table names and column names.
 * @status:success UPDATED
 */
const NamingStrategyPage: NgDocPage = {
    title: `Naming Strategy`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 9,
    route: 'naming-strategy'
};

export default NamingStrategyPage;

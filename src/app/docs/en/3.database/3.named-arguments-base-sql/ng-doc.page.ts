import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos provides a basic SQL builder that allows you to build SQL operations using named parameters,
 * and this chapter describes how to use this builder to perform operations such as SQL queries.
 * @status:info coming soon
 */
const NamedArgumentsBaseSqlPage: NgDocPage = {
    title: `Sql statement execution (named parameters)`,
    mdFile: './index.md',
    route: "named-arguments-base-sql",
    category: DatabaseCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NamedArgumentsBaseSqlPage;

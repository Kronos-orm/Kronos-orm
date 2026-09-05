import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * Kronos provides a basic SQL builder that allows you to build SQL operations using named parameters, and this chapter describes how to use this builder to perform operations such as SQL queries.
 * @status:success UPDATED
 */
const NamedArgumentsBaseSqlPage: NgDocPage = {
    title: `Native SQL`,
    mdFile: './index.md',
    route: "native-sql",
    category: QueryCategory,
    order: 9,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NamedArgumentsBaseSqlPage;

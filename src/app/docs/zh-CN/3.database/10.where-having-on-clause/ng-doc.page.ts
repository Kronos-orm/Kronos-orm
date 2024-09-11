import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const WhereHavingOnClausePage: NgDocPage = {
    title: `Criteria 条件`,
    mdFile: './index.md',
    route: "where-having-on-clause",
    category: DatabaseCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default WhereHavingOnClausePage;

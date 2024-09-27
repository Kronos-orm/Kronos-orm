import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * @status:info coming soon
 */
const WhereHavingOnClausePage: NgDocPage = {
    title: `Criteria conditions`,
    mdFile: './index.md',
    route: "where-having-on-clause",
    category: DatabaseCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default WhereHavingOnClausePage;

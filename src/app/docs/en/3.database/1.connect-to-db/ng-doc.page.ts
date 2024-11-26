import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter details how to create a database connection.
 * @status:success UPDATED
 */
const ConnectToDbPage: NgDocPage = {
    title: `Connect to DB`,
    mdFile: './index.md',
    route: "connect-to-db",
    category: DatabaseCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ConnectToDbPage;

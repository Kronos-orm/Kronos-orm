import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to create, delete, empty, and synchronize database tables.
 * @status:success UPDATED
 */
const DatabaseOperationPage: NgDocPage = {
    title: `Database Operation`,
    mdFile: './index.md',
    route: "database-operation",
    category: DatabaseCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseOperationPage;

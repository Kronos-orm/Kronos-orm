import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes how to synchronize a database table from KPojo metadata.
 * @status:info NEW
 */
const SchemaSyncPage: NgDocPage = {
    title: `Schema Sync`,
    mdFile: './index.md',
    route: "schema-sync",
    category: DatabaseCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SchemaSyncPage;

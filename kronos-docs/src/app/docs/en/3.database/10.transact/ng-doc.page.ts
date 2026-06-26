import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * In this chapter, we will introduce how to use database transactions in Kronos.
 * @status:info NEW
 */
const TransactPage: NgDocPage = {
    title: `Database Transactions`,
    mdFile: './index.md',
    route: "transact",
    category: DatabaseCategory,
    order: 10,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TransactPage;

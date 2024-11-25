import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将详细介绍如何创建数据库连接。
 * @status:info coming soon
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

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const ConnectToDbPage: NgDocPage = {
    title: `连接到数据库`,
    mdFile: './index.md',
    route: "connect-to-db",
    category: DatabaseCategory,
    order: 1,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ConnectToDbPage;

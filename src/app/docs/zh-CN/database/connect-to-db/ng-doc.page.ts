import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const ConnectToDbPage: NgDocPage = {
    title: `连接到数据库`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 0,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ConnectToDbPage;

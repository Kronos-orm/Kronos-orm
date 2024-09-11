import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const DatabaseOperationPage: NgDocPage = {
    title: `数据库操作`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseOperationPage;

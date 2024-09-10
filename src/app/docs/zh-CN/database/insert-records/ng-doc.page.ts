import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const InsertRecordsPage: NgDocPage = {
    title: `插入记录`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default InsertRecordsPage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const UpsertRecordsPage: NgDocPage = {
    title: `更新插入`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 7,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpsertRecordsPage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const UpdateRecordsPage: NgDocPage = {
    title: `更新记录`,
    mdFile: './index.md',
    route: "update-records",
    category: DatabaseCategory,
    order: 6,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default UpdateRecordsPage;

import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const DeleteRecordsPage: NgDocPage = {
    title: `删除记录`,
    mdFile: './index.md',
    route: "delete-records",
    category: DatabaseCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DeleteRecordsPage;

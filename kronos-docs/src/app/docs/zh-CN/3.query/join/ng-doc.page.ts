import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const SelectJoinTablesPage: NgDocPage = {
    title: `连表查询`,
    mdFile: './index.md',
    route: "join",
    category: QueryCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SelectJoinTablesPage;

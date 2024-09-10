import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const NamedArgumentsBaseSqlPage: NgDocPage = {
    title: `Sql语句执行（命名参数）`,
    mdFile: './index.md',
    category: DatabaseCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default NamedArgumentsBaseSqlPage;

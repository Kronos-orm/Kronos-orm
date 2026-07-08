import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const CreateDatabaseDialectPage: NgDocPage = {
    title: `创建数据库方言`,
    mdFile: './index.md',
    route: 'create-database-dialect',
    order: 9,
    category: DatabaseCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default CreateDatabaseDialectPage;

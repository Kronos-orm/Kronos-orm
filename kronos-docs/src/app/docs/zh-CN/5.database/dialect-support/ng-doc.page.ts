import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const DatabaseDialectPage: NgDocPage = {
    title: `数据库方言支持`,
    mdFile: './index.md',
    route: 'dialect-support',
    order: 6,
    category: DatabaseCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default DatabaseDialectPage;

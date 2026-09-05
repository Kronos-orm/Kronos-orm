import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍 select 投影、alias、生成结果形态、join 投影和派生投影源。
 * @status:info NEW
 */
const ProjectionPage: NgDocPage = {
    title: `投影`,
    mdFile: './index.md',
    route: "projection",
    category: QueryCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ProjectionPage;

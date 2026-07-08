import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter describes select projections, aliases, generated result shapes, join projections, and derived projection sources.
 * @status:info NEW
 */
const ProjectionPage: NgDocPage = {
    title: `Projection`,
    mdFile: './index.md',
    route: "projection",
    category: QueryCategory,
    order: 3,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ProjectionPage;

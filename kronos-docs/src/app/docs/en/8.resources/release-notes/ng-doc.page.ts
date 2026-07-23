import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * @status:primary 0.3.0
 */
const ChangeLogPage: NgDocPage = {
    title: `Release Notes`,
    mdFile: './index.md',
    route: "release-notes",
    category: ResourcesCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ChangeLogPage;

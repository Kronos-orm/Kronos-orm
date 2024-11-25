import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const ChangeLogPage: NgDocPage = {
    title: `Change Log`,
    mdFile: './index.md',
    route: "changelog",
    category: GettingStartedCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ChangeLogPage;

import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
/**
 * @status:primary 0.0.2
 */
const ChangeLogPage: NgDocPage = {
    title: `更新日志`,
    mdFile: './index.md',
    route: "changelog",
    category: GettingStartedCategory,
    order: 4,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default ChangeLogPage;

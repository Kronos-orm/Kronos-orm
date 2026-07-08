import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const InstallationPage: NgDocPage = {
    title: `安装`,
    mdFile: './index.md',
    route: "installation",
    category: GettingStartedCategory,
    order: 2,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent},
};

export default InstallationPage;

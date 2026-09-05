import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章收集常见安装、查询和运行时排查步骤。
 */
const TroubleshootingPage: NgDocPage = {
    title: `故障排查`,
    mdFile: './index.md',
    route: 'troubleshooting',
    order: 8,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default TroubleshootingPage;

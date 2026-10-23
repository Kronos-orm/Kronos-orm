import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章介绍 KEFS 配置、Kotlin 编辑器补全和编译器诊断。
 * @status:info 新
 */
const KefsPage: NgDocPage = {
    title: `KEFS 编辑器支持`,
    mdFile: './index.md',
    route: 'kefs',
    order: 2,
    category: ResourcesCategory,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default KefsPage;

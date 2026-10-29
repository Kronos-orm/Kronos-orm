import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {BenchmarkComponent} from "../../../../components/benchmark.component";

/**
 * 查看 JVM / MySQL 条件下的 ORM 性能对比、测试方法与详细测量数据。
 * @status:info 新
 */
const BenchmarkPage: NgDocPage = {
    title: `性能测试`,
    mdFile: './index.md',
    route: "benchmark",
    category: ResourcesCategory,
    order: 3,
    imports: [BenchmarkComponent],
    demos: {BenchmarkComponent}
};

export default BenchmarkPage;

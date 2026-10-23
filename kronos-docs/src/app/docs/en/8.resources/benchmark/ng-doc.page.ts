import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {BenchmarkComponent} from "../../../../components/benchmark.component";

/**
 * Compare ORM performance under JVM / MySQL, with methodology and detailed measurements.
 */
const BenchmarkPage: NgDocPage = {
    title: `Benchmark`,
    mdFile: './index.md',
    route: "benchmark",
    category: ResourcesCategory,
    order: 3,
    imports: [BenchmarkComponent],
    demos: {BenchmarkComponent}
};

export default BenchmarkPage;

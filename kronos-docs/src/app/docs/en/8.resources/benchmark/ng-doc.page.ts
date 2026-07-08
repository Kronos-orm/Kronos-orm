import {NgDocPage} from '@ng-doc/core';
import ResourcesCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import {BenchmarkComponent} from "../../../../components/benchmark.component";

const BenchmarkPage: NgDocPage = {
    title: `Benchmark`,
    mdFile: './index.md',
    route: "benchmark",
    category: ResourcesCategory,
    order: 3,
    imports: [AnimateLogoComponent, BenchmarkComponent],
    demos: {AnimateLogoComponent, BenchmarkComponent}
};

export default BenchmarkPage;

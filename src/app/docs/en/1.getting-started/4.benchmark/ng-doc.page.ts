import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import {BenchmarkComponent} from "../../../../components/benchmark.component";

const BenchmarkPage: NgDocPage = {
    title: `Benchmark`,
    mdFile: './index.md',
    route: "benchmark",
    category: GettingStartedCategory,
    order: 4,
    imports: [AnimateLogoComponent, BenchmarkComponent],
    demos: {AnimateLogoComponent, BenchmarkComponent}
};

export default BenchmarkPage;

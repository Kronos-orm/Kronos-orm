import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import {BenchmarkComponent} from "../../../../components/benchmark.component";

/**
 * 本章将介绍Kronos性能基准测试，主要包括Kronos的编译性能和运行性能，帮助您更好地了解Kronos的性能表现。
 * @status:info 新
 */
const BenchmarkPage: NgDocPage = {
    title: `性能测试`,
    mdFile: './index.md',
    route: "benchmark",
    category: GettingStartedCategory,
    order: 4,
    imports: [AnimateLogoComponent, BenchmarkComponent],
    demos: {AnimateLogoComponent, BenchmarkComponent}
};

export default BenchmarkPage;

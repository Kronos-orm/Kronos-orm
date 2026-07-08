import {NgDocPage} from '@ng-doc/core';
import QueryCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to use the **LOCK** feature of Kronos.
 * @status:success UPDATED
 */
const SomeLocksPage: NgDocPage = {
  title: `Locks`,
  mdFile: './index.md',
  route: "locks",
  order: 10,
  category: QueryCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

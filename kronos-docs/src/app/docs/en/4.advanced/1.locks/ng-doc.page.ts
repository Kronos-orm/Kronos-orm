import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to use the **LOCK** feature of Kronos.
 * @status:success UPDATED
 */
const SomeLocksPage: NgDocPage = {
  title: `Locks`,
  mdFile: './index.md',
  route: "some-locks",
  order: 1,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

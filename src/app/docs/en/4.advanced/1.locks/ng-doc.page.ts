import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This article will guide you on how to use the **LOCK** feature of Kronos.
 * @status:info coming soon
 */
const SomeLocksPage: NgDocPage = {
  title: `Locking`,
  mdFile: './index.md',
  route: "some-locks",
  order: 1,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

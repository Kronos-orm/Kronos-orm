import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何使用Kronos的**锁**功能。
 * @status:success 有更新
 */
const SomeLocksPage: NgDocPage = {
  title: `加锁机制`,
  mdFile: './index.md',
  route: "some-locks",
  order: 1,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

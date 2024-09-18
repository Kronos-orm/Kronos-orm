import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const SomeLocksPage: NgDocPage = {
  title: `使用asSql优化条件表达式性能`,
  mdFile: './index.md',
  route: "asSql",
  order: 2,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

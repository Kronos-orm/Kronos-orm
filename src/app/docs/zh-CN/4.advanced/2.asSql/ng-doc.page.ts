import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * `AsSql`是Kronos定义的SQL转换器，用于将Kotlin原生表达式转换为SQL语句。
 * @status:success 新
 */
const SomeLocksPage: NgDocPage = {
  title: `条件表达式中的asSql与短路操作`,
  mdFile: './index.md',
  route: "asSql",
  order: 2,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

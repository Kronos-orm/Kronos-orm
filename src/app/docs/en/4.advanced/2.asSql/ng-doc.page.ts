import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * `AsSql` is a Kronos-defined SQL converter for converting Kotlin native expressions to SQL statements.
 * @status:info coming soon
 */
const SomeLocksPage: NgDocPage = {
  title: `AsSql and short-circuit operations in conditional expressions`,
  mdFile: './index.md',
  route: "asSql",
  order: 2,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SomeLocksPage;

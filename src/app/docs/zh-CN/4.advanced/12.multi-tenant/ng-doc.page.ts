import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如使用Kronos的多租户功能
 * @status:info 新
 */
const MultiTenantPage: NgDocPage = {
  title: `多租户`,
  mdFile: './index.md',
  route: "multiTenant",
  order: 12,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default MultiTenantPage;

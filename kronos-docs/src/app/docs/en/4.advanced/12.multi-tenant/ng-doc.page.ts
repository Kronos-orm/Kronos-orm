import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to use the multi-tenant feature of Kronos.
 * @status:info NEW
 */
const MultiTenantPage: NgDocPage = {
  title: `Multi Tenant`,
  mdFile: './index.md',
  route: "multiTenant",
  order: 12,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default MultiTenantPage;

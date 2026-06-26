import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will guide you on how to perform type conversion between Map and KPojo or between different KPojo instances.
 * @status:success NEW
 */
const MapperToPage: NgDocPage = {
  title: `Map/KPojo Conversion`,
  mdFile: './index.md',
  route: "mapperTo",
  order: 10,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default MapperToPage;

import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本文将指导您如何进行Map与KPojo或者不同KPojo之间的类型转换
 * @status:info 新
 */
const MapperToPage: NgDocPage = {
  title: `Map/KPojo类型转换`,
  mdFile: './index.md',
  route: "mapperTo",
  order: 10,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default MapperToPage;

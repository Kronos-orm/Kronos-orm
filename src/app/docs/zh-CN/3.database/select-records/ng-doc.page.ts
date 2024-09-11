import {NgDocPage} from '@ng-doc/core';
import DatabaseCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const SelectRecordsPage: NgDocPage = {
  title: `查询记录`,
  mdFile: './index.md',
  category: DatabaseCategory,
  order: 8,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default SelectRecordsPage;

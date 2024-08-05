import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const QuickStartPage: NgDocPage = {
	title: `快速上手`,
	mdFile: './index.md',
  category: GettingStartedCategory,
  order: 1,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default QuickStartPage;

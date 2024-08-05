import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import {FeatureCardsComponent} from "../../../../components/feature-cards.component";

const WelcomePage: NgDocPage = {
	title: `欢迎 & 简介`,
	mdFile: './index.md',
  category: GettingStartedCategory,
  order: 0,
  imports: [AnimateLogoComponent, FeatureCardsComponent],
  demos: {AnimateLogoComponent, FeatureCardsComponent}
};

export default WelcomePage;

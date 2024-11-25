import {NgDocPage} from '@ng-doc/core';
import GettingStartedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";
import {FeatureCardsComponent} from "../../../../components/feature-cards.component";

const WelcomePage: NgDocPage = {
    title: `Introduce`,
    mdFile: './index.md',
    route: "introduce",
    category: GettingStartedCategory,
    order: 1,
    imports: [AnimateLogoComponent, FeatureCardsComponent],
    demos: {AnimateLogoComponent, FeatureCardsComponent}
};

export default WelcomePage;

import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const SerializeResolverPage: NgDocPage = {
    title: `自动序列化与反序列化`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 5,
    route: 'serialize-processor',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SerializeResolverPage;

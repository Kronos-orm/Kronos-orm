import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

const SerializeResolverPage: NgDocPage = {
    title: `序列化反序列化处理器`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 5,
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SerializeResolverPage;

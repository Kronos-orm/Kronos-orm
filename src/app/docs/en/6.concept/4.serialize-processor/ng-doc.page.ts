import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * @status:info coming soon
 */
const SerializeProcessorPage: NgDocPage = {
    title: `Automatic Serialization and Deserialization`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 5,
    route: 'serialize-processor',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default SerializeProcessorPage;

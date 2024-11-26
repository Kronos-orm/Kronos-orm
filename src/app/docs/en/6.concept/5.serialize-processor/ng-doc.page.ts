import {NgDocPage} from '@ng-doc/core';
import ConceptCategory from "../ng-doc.category";

/**
 * `KronosSerializeProcessor` is a serialization processor interface defined by Kronos for serialization and deserialization conversions between strings and Kotlin entity classes.
 * @status:info NEW
 */
const SerializeProcessorPage: NgDocPage = {
    title: `Serialization and Deserialization`,
    mdFile: './index.md',
    category: ConceptCategory,
    order: 5,
    route: 'serialize-processor'
};

export default SerializeProcessorPage;

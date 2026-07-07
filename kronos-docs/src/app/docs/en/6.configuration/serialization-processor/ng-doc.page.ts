import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * `KronosSerializeProcessor` is a serialization processor interface defined by Kronos for serialization and deserialization conversions between strings and Kotlin entity classes.
 * @status:info NEW
 */
const SerializeProcessorPage: NgDocPage = {
    title: `Serialization and Deserialization`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 2,
    route: 'serialization-processor'
};

export default SerializeProcessorPage;

import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * `KronosSerializeProcessor`是Kronos定义的序列化解析器接口，用于字符串和Kotlin实体类之间的序列化和反序列化转换。
 * @status:info 新
 */
const SerializeProcessorPage: NgDocPage = {
    title: `自动序列化与反序列化`,
    mdFile: './index.md',
    category: ConfigurationCategory,
    order: 2,
    route: 'serialization-processor'
};

export default SerializeProcessorPage;

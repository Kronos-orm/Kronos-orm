import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * 面向领域类型的自定义值映射。
 * @status:info
 */
const ValueCodecPage: NgDocPage = {
    title: `自定义值映射`,
    mdFile: './index.md',
    route: 'value-codec',
    category: ConfigurationCategory,
    order: 6
};

export default ValueCodecPage;

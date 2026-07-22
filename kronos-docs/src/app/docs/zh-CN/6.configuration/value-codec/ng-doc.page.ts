import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * Kronos 统一值转换的注册、匹配、优先级与生命周期。
 * @status:info NEW
 */
const ValueCodecPage: NgDocPage = {
    title: `值编解码器`,
    mdFile: './index.md',
    route: 'value-codec',
    category: ConfigurationCategory,
    order: 6
};

export default ValueCodecPage;

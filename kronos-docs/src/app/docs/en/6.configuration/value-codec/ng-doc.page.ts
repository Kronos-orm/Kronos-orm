import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * User-facing custom domain value mapping.
 * @status:info
 */
const ValueCodecPage: NgDocPage = {
    title: `Custom Value Mapping`,
    mdFile: './index.md',
    route: 'value-codec',
    category: ConfigurationCategory,
    order: 6
};

export default ValueCodecPage;

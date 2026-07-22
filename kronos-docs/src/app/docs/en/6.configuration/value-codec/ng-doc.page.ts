import {NgDocPage} from '@ng-doc/core';
import ConfigurationCategory from "../ng-doc.category";

/**
 * Unified value conversion registration, matching, priority, and lifecycle.
 * @status:info NEW
 */
const ValueCodecPage: NgDocPage = {
    title: `Value Codec`,
    mdFile: './index.md',
    route: 'value-codec',
    category: ConfigurationCategory,
    order: 6
};

export default ValueCodecPage;

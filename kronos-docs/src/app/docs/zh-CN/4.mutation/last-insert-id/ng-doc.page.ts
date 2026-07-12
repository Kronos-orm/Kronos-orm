import {NgDocPage} from '@ng-doc/core';
import MutationCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 从 insert 结果读取数据库生成的自增主键 ID。
 * @status:info 新
 */
const LastInsertIdPage: NgDocPage = {
    title: `获取最后插入的 ID`,
    mdFile: './index.md',
    category: MutationCategory,
    order: 6,
    route: 'last-insert-id',
    imports: [AnimateLogoComponent],
    demos: {AnimateLogoComponent}
};

export default LastInsertIdPage;

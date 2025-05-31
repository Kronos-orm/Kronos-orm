import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何注册任务事件以实现任务的事件监听和处理。
 * @status:warning WIP
 */
const TaskEventPage: NgDocPage = {
  title: `任务事件监听`,
  mdFile: './index.md',
  route: "multiTenant",
  order: 12,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default TaskEventPage;

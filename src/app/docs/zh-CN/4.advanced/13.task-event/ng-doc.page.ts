import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * 本章将介绍如何注册任务事件以实现任务的事件监听和处理。
 * @status:info 新
 */
const TaskEventPage: NgDocPage = {
  title: `任务事件系统`,
  mdFile: './index.md',
  route: "task-event",
  order: 13,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default TaskEventPage;

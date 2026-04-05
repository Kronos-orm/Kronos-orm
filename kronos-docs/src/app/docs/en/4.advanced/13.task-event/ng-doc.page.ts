import {NgDocPage} from '@ng-doc/core';
import AdvancedCategory from "../ng-doc.category";
import {AnimateLogoComponent} from "../../../../components/animate-logo.component";

/**
 * This chapter will introduce how to register task events to achieve event listening and processing for tasks.
 * @status:warning WIP
 */
const TaskEventPage: NgDocPage = {
  title: `Task events handling`,
  mdFile: './index.md',
  route: "multiTenant",
  order: 12,
  category: AdvancedCategory,
  imports: [AnimateLogoComponent],
  demos: {AnimateLogoComponent}
};

export default TaskEventPage;

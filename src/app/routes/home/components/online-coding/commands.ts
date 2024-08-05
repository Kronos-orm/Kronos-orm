import {
  deleteTask1,
  deleteTask2,
  deleteTask3,
  insertTask1,
  insertTask2,
  selectTask1,
  selectTask2,
  selectTask3, selectTask4,
  updateTask1,
  updateTask2,
  updateTask3,
  updateTask4,
  upsertTask
} from "./executors";
import {$dataClass, $delete, $insert, $select, $update, $upsert} from "./codes";
import {Product} from "./products";


export const $exec: {
  [key: string]: (data: any[], callBack: (all: Product[], selected?: Product[])  => void) => string[]
} = {
  'execute-select --task=01': selectTask1,
  'execute-select --task=02': selectTask2,
  'execute-select --task=03': selectTask3,
  'execute-select --task=04': selectTask4,
  'execute-insert --task=01': insertTask1,
  'execute-insert --task=02': insertTask2,
  'execute-delete --task=01': deleteTask1,
  'execute-delete --task=02': deleteTask2,
  'execute-delete --task=03': deleteTask3,
  'execute-update --task=01': updateTask1,
  'execute-update --task=02': updateTask2,
  'execute-update --task=03': updateTask3,
  'execute-update --task=04': updateTask4,
  'execute-upsert --task=01': upsertTask,
  'execute-upsert --task=02': upsertTask,
}

export interface Command {
  label: string,
  icon: string,
  doc: string,
  rowNum: number,
  tip: { [key: number]: string },
  slice?: { [key: number]: string }
}

export const commands: Command[] = [
  {
    label: 'Product.kt', icon: 'pi pi-code', doc: $dataClass, rowNum: $dataClass.split('\n').length,
    tip: {
      0: "KPOJO_DESCRIPTION[0]",
      1: "KPOJO_DESCRIPTION[1]",
      3: "KPOJO_DESCRIPTION[2]",
      7: "KPOJO_DESCRIPTION[3]"
    },
  },
  {
    label: 'Select.kt', icon: 'pi pi-table', doc: $select, rowNum: $select.split('\n').length,
    tip: {},
    slice: {
      0: 'execute-select --task=01',
      4: 'execute-select --task=02',
      9: 'execute-select --task=03',
      11: 'execute-select --task=04'
    }
  },
  {
    label: 'Insert.kt', icon: 'pi pi-plus', doc: $insert, rowNum: $insert.split('\n').length,
    tip: {},
    slice: {
      0: 'execute-insert --task=01',
      2: 'execute-insert --task=02'
    }
  },
  {
    label: 'Delete.kt', icon: 'pi pi-minus', doc: $delete, rowNum: $delete.split('\n').length,
    tip: {},
    slice: {
      0: 'execute-delete --task=01',
      4: 'execute-delete --task=02',
      8: 'execute-delete --task=03'
    }
  },
  {
    label: 'Update.kt', icon: 'pi pi-pencil', doc: $update, rowNum: $update.split('\n').length,
    tip: {},
    slice: {
      0: 'execute-update --task=01',
      5: 'execute-update --task=02',
      10: 'execute-update --task=03',
      18: 'execute-update --task=04'
    }
  },
  {
    label: 'Upsert.kt', icon: 'pi pi-refresh', doc: $upsert, rowNum: $upsert.split('\n').length,
    tip: {},
    slice: {
      1: 'execute-upsert --task=01',
      5: 'execute-upsert --task=02'
    }
  }
]

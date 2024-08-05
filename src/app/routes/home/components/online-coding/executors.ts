import {Product} from "./products";

export const selectTask1 = (data: Product[], callBack: (all: Product[], selected: Product[]) => void) => {
  const selectedRows = data.filter(item => item.name === "Sunglasses");
  callBack(data, selectedRows);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'SELECT `code`, `quantity` FROM `tb_product` WHERE `name` = :name'}`,
    `Params: {name: "Sunglasses"}`,
    `-----------------------------------------`,
    `Result: ${JSON.stringify(selectedRows.map(item => {
      return {code: item.code, quantity: item.quantity}
    }))}`
  ]
}

export const selectTask2 = (data: Product[], callBack: (all: Product[], selected: Product[]) => void) => {
  const selectedRows = data.filter(item => ["Sunglasses", "Bamboo Watch"].includes(item.name));
  callBack(data, selectedRows);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'SELECT `code`, `name`, `category`, `quantity` FROM `tb_product` WHERE `name` IN (:name)'}`,
    `Params: {name: ["Sunglasses", "Bamboo Watch"]}`,
    `-----------------------------------------`,
    `Result: ${JSON.stringify(selectedRows.map(item => {
      return {code: item.code, name: item.name, category: item.category, quantity: item.quantity}
    }))}
    `
  ];
}

export const selectTask3 = (data: Product[], callBack: (all: Product[], selected: Product[]) => void) => {
  const selectedRows = data.filter(item => item.code === "7F9V0BpQ2W");
  callBack(data, selectedRows);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'SELECT `code`, `name`, `category`, `quantity` FROM `tb_product` WHERE `code` = :code'}`,
    `Params: {code: "7F9V0BpQ2W"}`,
    `-----------------------------------------`,
    `Result: ${JSON.stringify(selectedRows.map(item => {
      return {code: item.code, name: item.name, category: item.category, quantity: item.quantity}
    }))}
    `
  ]
}

export const selectTask4 = (data: Product[], callBack: (all: Product[], selected: Product[]) => void) => {
  const selectedRows = data.filter(item => item.quantity > 10);
  callBack(data, selectedRows);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'SELECT `code`, `name`, `category`, `quantity` FROM `tb_product` WHERE `quantity` > :quantityMin'}`,
    `Params: {quantityMin: 10}`,
    `-----------------------------------------`,
    `Result: ${JSON.stringify(selectedRows.map(item => {
      return {code: item.code, name: item.name, category: item.category, quantity: item.quantity}
    }))}
    `
  ]
}

export const insertTask1 = (data: any[], callBack: (data: any[], selected: Product[]) => void) => {
  if (data.find(item => item.code === "a3c2a3fv27")) return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'INSERT INTO `tb_product` (`code`, `name`, `category`, `quantity`) VALUES (:code, :name, :category, :quantity)'}`,
    `Params: {code: "a3c2a3fv27", name: "Sunglasses", category: "Fashion", quantity: 3}`,
    `-----------------------------------------`,
    `Insert failed, duplicate key found.`,
    `Result: affected rows: 0`
  ];
  const row = {code: "a3c2a3fv27", name: "Sunglasses", category: "Fashion", quantity: 3};
  callBack([...data, row], [row as Product])
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'INSERT INTO `tb_product` (`code`, `name`, `category`, `quantity`) VALUES (:code, :name, :category, :quantity)'}`,
    `Params: {code: "a3c2a3fv27", name: "Sunglasses", category: "Fashion", quantity: 3}`,
    `-----------------------------------------`,
    `Result: affected rows: 1`
  ]
}

export const insertTask2 = (data: any[], callBack: (data: any[], selected: any[]) => void) => {
  if (data.find(item => ["c0a45pzo2a", "q1fj099afc"].includes(item.code))) return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'INSERT INTO `tb_product` (`code`, `name`, `category`, `quantity`) VALUES (:code, :name, :category, :quantity)'}`,
    `Params: [{code: "c0a45pzo2a", name: "Apple Watch", category: "Fashion", quantity: 3},`,
    `\t{code: "q1fj099afc", name: "Portable Speakers", category: "Electronics", quantity: 2}]`,
    `-----------------------------------------`,
    `Insert failed, duplicate key found.`,
    `Result: affected rows: 0`
  ];
  const rows= [
    {code: "c0a45pzo2a", name: "Apple Watch", category: "Fashion", quantity: 3},
    {code: "q1fj099afc", name: "Portable Speakers", category: "Electronics", quantity: 2}
  ] as Product[] ;
  callBack([...data, ...rows], rows)
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'INSERT INTO `tb_product` (`code`, `name`, `category`, `quantity`) VALUES (:code, :name, :category, :quantity)'}`,
    `Params: [{code: "c0a45pzo2a", name: "Apple Watch", category: "Fashion", quantity: 3},`,
    `\t{code: "q1fj099afc", name: "Portable Speakers", category: "Electronics", quantity: 2}]`,
    `-----------------------------------------`,
    `Result: affected rows: 2`
  ]
}

export const deleteTask1 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.code === "a3c2a3fv27").length;
  callBack(data.filter(item => item.code !== "a3c2a3fv27"));
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'DELETE FROM `tb_product` WHERE `code` = :code'}`,
    `Params: {code: "a3c2a3fv27"}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const deleteTask2 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.category === "Fashion" && item.quantity < 4).length;
  callBack(data.filter(item => !(item.category === "Fashion" && item.quantity < 4)));
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'DELETE FROM `tb_product` WHERE `category` = :category AND `quantity` < :quantityMax'}`,
    `Params: {category: "Fashion", quantityMax: 3}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const deleteTask3 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.code === "a3c2a3fv27").length;
  callBack(data.filter(item => item.code !== "a3c2a3fv27"));
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'DELETE FROM `tb_product` WHERE `code` = :code'}`,
    `Params: {code: "a3c2a3fv27"}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const updateTask1 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.code === "a3c2a3fv27").length;
  data.forEach(item => {
    if (item.code === "a3c2a3fv27") {
      item.name = "Apple Watch"
      item.category = "Electronics"
      item.quantity = 0
    }
  })
  callBack([...data]);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'UPDATE `tb_product` SET `name` = :nameNew, `category` = :categoryNew, `quantity` = :quantityNew WHERE `code` = :code'}`,
    `Params: {code: "a3c2a3fv27", nameNew: "Apple Watch", categoryNew: "Electronics", quantityNew: 0}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}
export const updateTask2 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.code === "a3c2a3fv27").length;
  data.forEach(item => {
    if (item.code === "a3c2a3fv27") {
      item.name = "Umbrella"
      item.quantity = 0
    }
  })
  callBack([...data]);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'UPDATE `tb_product` SET `name` = :nameNew, `quantity` = :quantityNew WHERE `code` = :code'}`,
    `Params: {code: "a3c2a3fv27", nameNew: "Umbrella", quantityNew: 0}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const updateTask3 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.code === "a3c2a3fv27").length;
  data.forEach(item => {
    if (item.code === "a3c2a3fv27") {
      item.name = "Basketball"
      item.category = "Sports"
    }
  })
  callBack([...data]);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'UPDATE `tb_product` SET `name` = :nameNew, `category` = :categoryNew WHERE `code` = :code'}`,
    `Params: {code: "a3c2a3fv27", nameNew: "Basketball", categoryNew: "Sports"}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const updateTask4 = (data: any[], callBack: (data: any[]) => void) => {
  const affectRows = data.filter(item => item.name.includes("Watch")).length;
  data.forEach(item => {
    if (item.name.includes("Watch")) {
      item.quantity = 0
    }
  })
  callBack([...data]);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'UPDATE `tb_product` SET `quantity` = :quantityNew WHERE `name` LIKE :name'}`,
    `Params: {name: "%Watch%", quantityNew: 0}`,
    `-----------------------------------------`,
    `Result: affected rows: ${affectRows}`
  ]
}

export const upsertTask = (data: any[], callBack: (data: any[]) => void) => {
  if (data.find(item => item.code === "a3c2a3fv27")) {
    data.forEach(item => {
      if (item.code === "a3c2a3fv27") {
        item.name = "Sunglasses"
        item.category = "Fashion"
        item.quantity = 3
      }
    })
    callBack([...data]);
    return [
      `---------------Kronos Task---------------`,
      `Sql: \t${'UPDATE `tb_product` SET `name` = :nameNew, `category` = :categoryNew, `quantity` = :quantityNew WHERE `code` = :code'}`,
      `Params: {code: "a3c2a3fv27", nameNew: "Sunglasses", categoryNew: "Fashion", quantityNew: 3}`,
      `-----------------------------------------`,
      `Result: affected rows: 1`
    ];
  }
  callBack([...data, {
    code: "a3c2a3fv27",
    name: "Sunglasses",
    category: "Fashion",
    quantity: 3
  }]);
  return [
    `---------------Kronos Task---------------`,
    `Sql: \t${'INSERT INTO `tb_product` (`code`, `name`, `category`, `quantity`) VALUES (:code, :name, :category, :quantity)'}`,
    `Params: {code: "a3c2a3fv27", name: "Sunglasses", category: "Fashion", quantity: 3}`,
    `-----------------------------------------`,
    `Result: affected rows: 1`
  ]
}

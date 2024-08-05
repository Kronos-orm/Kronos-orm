export interface Product {
  id: string;
  code: string;
  name: string;
  description: string;
  image: string;
  price: number;
  category: string;
  quantity: number;
  inventoryStatus: string;
  rating: number;
}

export const products: Product[] = [
  {
    "id": "1000",
    "code": "7F9V0BpQ2W",
    "name": "Bamboo Watch",
    "description": "Product Description",
    "image": "bamboo-watch.jpg",
    "price": 65,
    "category": "Accessories",
    "quantity": 24,
    "inventoryStatus": "INSTOCK",
    "rating": 5
  },
  {
    "id": "1001",
    "code": "5a3tR8xY9E",
    "name": "Leather Wallet",
    "description": "Stylish leather wallet",
    "image": "leather-wallet.jpg",
    "price": 45,
    "category": "Accessories",
    "quantity": 18,
    "inventoryStatus": "INSTOCK",
    "rating": 4
  },
  {
    "id": "1002",
    "code": "2K7s6R0j1N",
    "name": "Silver Bracelet",
    "description": "Elegant silver bracelet",
    "image": "silver-bracelet.jpg",
    "price": 80,
    "category": "Jewelry",
    "quantity": 12,
    "inventoryStatus": "INSTOCK",
    "rating": 4.5
  },
  {
    "id": "1003",
    "code": "8T4w7X1c0Y",
    "name": "Sunglasses",
    "description": "UV protection sunglasses",
    "image": "sunglasses.jpg",
    "price": 55,
    "category": "Accessories",
    "quantity": 30,
    "inventoryStatus": "INSTOCK",
    "rating": 4
  },
  {
    "id": "1004",
    "code": "3M5b1Z8a0R",
    "name": "Leather Bag",
    "description": "Stylish leather bag",
    "image": "leather-bag.jpg",
    "price": 120,
    "category": "Bags",
    "quantity": 10,
    "inventoryStatus": "INSTOCK",
    "rating": 4.2
  },
  {
    "id": "1005",
    "code": "4J8c5Z1w9T",
    "name": "Smart Watch",
    "description": "Intelligent smart watch",
    "image": "smart-watch.jpg",
    "price": 150,
    "category": "Electronics",
    "quantity": 15,
    "inventoryStatus": "INSTOCK",
    "rating": 4.8
  },
  {
    "id": "1006",
    "code": "HATrU8EB5V",
    "name": "Diamond Ring",
    "description": "Exquisite diamond ring",
    "image": "diamond-ring.jpg",
    "price": 500,
    "category": "Jewelry",
    "quantity": 5,
    "inventoryStatus": "INSTOCK",
    "rating": 4.9
  },
  {
    "id": "1007",
    "code": "wgASVUx1cO",
    "name": "Headphones",
    "description": "High-quality headphones",
    "image": "headphones.jpg",
    "price": 80,
    "category": "Electronics",
    "quantity": 20,
    "inventoryStatus": "INSTOCK",
    "rating": 4.5
  },
  {
    "id": "1008",
    "code": "rEzEXV2hiw",
    "name": "Backpack",
    "description": "Spacious backpack",
    "image": "backpack.jpg",
    "price": 70,
    "category": "Bags",
    "quantity": 25,
    "inventoryStatus": "INSTOCK",
    "rating": 4.3
  },
  {
    "id": "1009",
    "code": "LtwfbSGJxI",
    "name": "Silver Earrings",
    "description": "Elegant silver earrings",
    "image": "silver-earrings.jpg",
    "price": 40,
    "category": "Jewelry",
    "quantity": 15,
    "inventoryStatus": "INSTOCK",
    "rating": 4.6
  },
  {
    "id": "1010",
    "code": "gdK9PAS5Xq",
    "name": "Wireless Mouse",
    "description": "Ergonomic wireless mouse",
    "image": "wireless-mouse.jpg",
    "price": 25,
    "category": "Electronics",
    "quantity": 35,
    "inventoryStatus": "INSTOCK",
    "rating": 4.2
  },
  {
    "id": "1011",
    "code": "uorP6F8oFM",
    "name": "Travel Bag",
    "description": "Durable travel bag",
    "image": "travel-bag.jpg",
    "price": 90,
    "category": "Bags",
    "quantity": 8,
    "inventoryStatus": "INSTOCK",
    "rating": 4
  },
  {
    "id": "1012",
    "code": "zPwRspUC5n",
    "name": "Gold Necklace",
    "description": "Luxurious gold necklace",
    "image": "gold-necklace.jpg",
    "price": 300,
    "category": "Jewelry",
    "quantity": 7,
    "inventoryStatus": "INSTOCK",
    "rating": 4.8
  },
  {
    "id": "1013",
    "code": "gYYxVVbotb",
    "name": "Bluetooth Speaker",
    "description": "Portable Bluetooth speaker",
    "image": "bluetooth-speaker.jpg",
    "price": 70,
    "category": "Electronics",
    "quantity": 15,
    "inventoryStatus": "INSTOCK",
    "rating": 4.4
  },
  {
    "id": "1014",
    "code": "FrZWFVfGkJ",
    "name": "Laptop Bag",
    "description": "Stylish laptop bag",
    "image": "laptop-bag.jpg",
    "price": 50,
    "category": "Bags",
    "quantity": 20,
    "inventoryStatus": "INSTOCK",
    "rating": 4.1
  },
  {
    "id": "1015",
    "code": "qQaEHwHy2h",
    "name": "Pearl Bracelet",
    "description": "Beautiful pearl bracelet",
    "image": "pearl-bracelet.jpg",
    "price": 70,
    "category": "Jewelry",
    "quantity": 12,
    "inventoryStatus": "INSTOCK",
    "rating": 4.7
  },
  {
    "id": "1016",
    "code": "aBTsR9MDYW",
    "name": "Digital Camera",
    "description": "High-resolution digital camera",
    "image": "digital-camera.jpg",
    "price": 200,
    "category": "Electronics",
    "quantity": 10,
    "inventoryStatus": "INSTOCK",
    "rating": 4.6
  },
  {
    "id": "1017",
    "code": "fva7jGYvEP",
    "name": "Tote Bag",
    "description": "Stylish tote bag",
    "image": "tote-bag.jpg",
    "price": 60,
    "category": "Bags",
    "quantity": 15,
    "inventoryStatus": "INSTOCK",
    "rating": 4.2
  },
  {
    "id": "1018",
    "code": "jVv3d4mSH7",
    "name": "Diamond Necklace",
    "description": "Exquisite diamond necklace",
    "image": "diamond-necklace.jpg",
    "price": 600,
    "category": "Jewelry",
    "quantity": 4,
    "inventoryStatus": "INSTOCK",
    "rating": 4.9
  },
  {
    "id": "1019",
    "code": "rpaSf38DWd",
    "name": "Wireless Headphones",
    "description": "Wireless noise-canceling headphones",
    "image": "wireless-headphones.jpg",
    "price": 120,
    "category": "Electronics",
    "quantity": 8,
    "inventoryStatus": "INSTOCK",
    "rating": 4.7
  }
]

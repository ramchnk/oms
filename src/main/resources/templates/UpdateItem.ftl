{
	"requestType": "updateItem",
	"SKU": "${exchange.in.body}",
	"accountNumber": "${exchange.properties.accountNumber}",
	"siteNicknames": ["${exchange.properties.nickNameID}"],
	"fieldsToUpdate": ["salePrice", "salesStartDate", "price"],
	"channel": "lazada"
}

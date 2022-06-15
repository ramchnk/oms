<#assign payloadList = []>
<#if exchange.properties.payloadList??>
	<#assign payloadList=exchange.properties.payloadList>
</#if>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
<Product>
<Skus>

	<#list payloadList as listObj>
    <Sku>
        <#if listObj.skuID?? && listObj.itemID??>
            <SkuId>${listObj.skuID?js_string}</SkuId>
            <ItemId>${listObj.itemID?js_string}</ItemId>
        </#if>
        <SellerSku>${listObj.sellerSKU}</SellerSku>
       <Price>${(listObj.itemAmount.amount/100)?string("0.00")}</Price>
			<#if listObj.salePrice??>
			<#setting time_zone=exchange.properties.timeZoneOffset>
				<SalePrice>${(listObj.salePrice.amount/100)?string("0.00")}</SalePrice>
				<SaleStartDate>${(listObj.saleStartDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleStartDate>
				<SaleEndDate>${(listObj.saleEndDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleEndDate>
			</#if>
    </Sku>
    </#list>
</Skus>
</Product>
</Request>
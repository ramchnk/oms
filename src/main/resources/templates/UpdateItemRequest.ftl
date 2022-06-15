<#assign inventoryList = []>
<#if exchange.properties.inventoryVariantDetails?? && exchange.properties.inventoryVariantDetails?size != 0>
	<#assign inventoryList = exchange.properties.inventoryVariantDetails>
<#elseif exchange.properties.inventoryDetails?? && exchange.properties.inventoryDetails?size != 0>
	<#assign inventoryList = exchange.properties.inventoryDetails>
<#elseif exchange.properties.inventory??>
	<#assign inventoryList = [exchange.properties.inventory]>
</#if>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
<Product>
<#if exchange.properties.itemID?? && exchange.properties.isQuantityUpdate?? && exchange.properties.isQuantityUpdate == true>
     <ItemId>${exchange.properties.itemID}</ItemId>
</#if>
<Skus>
    <#if exchange.properties.isQuantityUpdate?? && exchange.properties.isQuantityUpdate == true>
    	<Sku>
             <SellerSku><![CDATA[${exchange.properties.refrenceID}]]></SellerSku>
             <#if exchange.properties.statusToUpdate??>
                  <Status>${exchange.properties.statusToUpdate}</Status>
             </#if>
             <#if exchange.properties.skuID??>
                  <SkuId>${exchange.properties.skuID}</SkuId>
             </#if>
             <#if exchange.properties.isUpdateStockViaProductUpdateApi?? && exchange.properties.isUpdateStockViaProductUpdateApi == true>
                  <quantity>${exchange.properties.overAllQuantity?string("0")}</quantity>
             </#if>
        </Sku>
    <#else>
	<#list inventoryList as inventory>
	   <#if inventory.SKU?contains("-") || (!inventory.SKU?contains("-") && exchange.properties.hasVariants == false)>
		<#assign lazada = inventory.lazada>
		<#assign refrenceID = "">
		<#assign skuID = "">
		<#assign itemID = "">
		<#if lazada?is_sequence>
			<#if lazada[0].refrenceID??>
				<#assign refrenceID = lazada[0].refrenceID>
				<#assign status = lazada[0].status>
			</#if>
			<#if lazada[0].skuID??>
				<#assign skuID = lazada[0].skuID>
			</#if>
			<#if lazada[0].itemID??>
				<#assign itemID = lazada[0].itemID>
			</#if>
		<#else>
			<#if lazada.refrenceID??>
				<#assign refrenceID = lazada.refrenceID>
				<#assign status = lazada.status>
			</#if>
			<#if lazada.skuID??>
				<#assign skuID = lazada.skuID>
			</#if>
			<#if lazada.itemID??>
				<#assign itemID = lazada.itemID>
			</#if>
		</#if>
		<#if refrenceID?has_content>
		<Sku>
			<#if skuID == "" && exchange.properties.inventory?? && exchange.properties.inventory.lazada?? && exchange.properties.inventory.lazada.skuID??>
				<#assign skuID = exchange.properties.inventory.lazada.skuID>
			</#if>
			<#if itemID == "" && exchange.properties.inventory?? && exchange.properties.inventory.lazada?? && exchange.properties.inventory.lazada.itemID??>
				<#assign itemID = exchange.properties.inventory.lazada.itemID>
			</#if>
			<#if exchange.properties.isStatusUpdate?? && exchange.properties.isStatusUpdate == true
				&& exchange.properties.isChildVariantStatusUpdate == false && exchange.properties.requestType != "batchEditItem">
				<#assign status = exchange.properties.inventory.lazada.status>
			</#if>
			<#if itemID != "" && skuID != "">
					<ItemId>${itemID}</ItemId>
					<SkuId>${skuID}</SkuId>
			</#if>
			<SellerSku>${refrenceID}</SellerSku>
			<#if exchange.properties.fieldsToUpdate?seq_contains("status")>
				<#if exchange.properties.updateToStatus??>
					<Status>${exchange.properties.updateToStatus}</Status>
				<#elseif status?string == "N">
					<Status>inactive</Status>
				<#else>
					<Status>active</Status>
				</#if>
			<#else>
				<#if exchange.properties.fieldsToUpdate?seq_contains("quantity") || exchange.properties.fieldsToUpdate?seq_contains("bufferQuantity")>
					<#assign quantity = lazada.noOfItem>
					<#assign bufferQuantity = 0>
					<#if lazada.bufferQuantity??>
						<#assign bufferQuantity = lazada.bufferQuantity>
					</#if>
					<#assign quantity = quantity + bufferQuantity>
					<#if exchange.properties.qtyUnderProcessing??>
						<#assign quantity = quantity + exchange.properties.qtyUnderProcessing>
					</#if>
					<#if quantity < 0>
						<#assign quantity = 0>
					</#if>
					<#if exchange.properties.isUpdateQuantityByDiff?? && exchange.properties.isUpdateQuantityByDiff == true>
						<#assign quantityDiff = exchange.properties.quantityDiff>
						<#assign quantityDiff = quantityDiff + bufferQuantity>
						<SellableQuantity>${quantityDiff?string("0")}</SellableQuantity>
					<#elseif exchange.properties.isUpdateSellableStock?? && exchange.properties.isUpdateSellableStock == true>
					    <SellableQuantity>${quantity?string("0")}</SellableQuantity>
					<#else>
					    <Quantity>${quantity?string("0")}</Quantity>
					</#if>
				</#if>
				<#if exchange.properties.fieldsToUpdate?seq_contains("price")>
					<Price>${(lazada.itemAmount.amount/100)?string("0.00")}</Price>
				</#if>
				<#if exchange.properties.fieldsToUpdate?seq_contains("salePrice") || exchange.properties.fieldsToUpdate?seq_contains("price")>
					<#if lazada.salePrice??>
						<SalePrice>${(lazada.salePrice.amount/100)?string("0.00")}</SalePrice>
						<#setting time_zone=exchange.properties.timeZoneOffset>
						<SaleStartDate>${(lazada.saleStartDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleStartDate>
						<SaleEndDate>${(lazada.saleEndDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleEndDate>
					<#elseif exchange.properties.isSalePriceRemove?? && exchange.properties.isSalePriceRemove>
						<SalePrice>${(lazada.itemAmount.amount/100)?string("0.00")}</SalePrice>
						<SaleStartDate>${(exchange.properties.yesterdayInSeconds*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleStartDate>
						<SaleEndDate>${(exchange.properties.yesterdayInSeconds*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleEndDate>
					</#if>
				</#if>
			</#if>
		</Sku>
		</#if>
	  </#if>
	</#list>
	</#if>
</Skus>
</Product>
</Request>
	
	
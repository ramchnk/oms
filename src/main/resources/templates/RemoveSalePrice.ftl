<#assign inventoryList = []>
<#if exchange.properties.inventoryDetails?? && exchange.properties.inventoryDetails?size != 0>
	<#assign inventoryList = exchange.properties.inventoryDetails>
<#else>
	<#assign inventoryList = [exchange.properties.inventory]>
</#if>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
	<Product>
		<Skus>
			<#list inventoryList as inventory>
			   <#if inventory.SKU?contains("-") || (!inventory.SKU?contains("-") && exchange.properties.hasVariants == false)>
				<#assign lazada = inventory.lazada>
				<Sku>
					<#if lazada.skuID?? && lazada.itemID??>
						<ItemId>${lazada.itemID}</ItemId>
						<SkuId>${lazada.skuID}</SkuId>
					</#if>
					<SellerSku>${lazada.refrenceID}</SellerSku>
					<SalePrice>${(lazada.itemAmount.amount/100)?string("0.00")}</SalePrice>
					<SaleStartDate>${(exchange.properties.yesterdayInSeconds*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleStartDate>
					<SaleEndDate>${(exchange.properties.yesterdayInSeconds*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</SaleEndDate>
				</Sku>
			  </#if>
			</#list>
		</Skus>
	</Product>
</Request>
	
	
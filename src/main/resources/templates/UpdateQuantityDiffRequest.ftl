<?xml version='1.0' encoding='UTF-8' ?>
<Request>
	<Product>
		<Skus>
			<Sku>
				<#if exchange.properties.itemID?? && exchange.properties.skuID??>
					<ItemId>${exchange.properties.itemID}</ItemId>
					<SkuId>${exchange.properties.skuID}</SkuId>
				</#if>
				<SellerSku><![CDATA[${exchange.properties.refrenceID}]]></SellerSku>
				<SellableQuantity>${exchange.properties.quantityDiff?string("0")}</SellableQuantity>
			</Sku>
		</Skus>
	</Product>
</Request>
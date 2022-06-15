<#assign list = []>
<#if exchange.properties.list??>
	<#assign list=exchange.properties.list>
</#if>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
<Product>
<Skus>
<#if exchange.properties.isQuantityUpdate?? && exchange.properties.isQuantityUpdate == true>
        <Sku>
             <#if exchange.properties.itemID?? && exchange.properties.skuID??>
                 <ItemId>${exchange.properties.itemID}</ItemId>
                 <SkuId>${exchange.properties.skuID}</SkuId>
             </#if>
             <SellerSku><![CDATA[${exchange.properties.refrenceID}]]></SellerSku>
             <#if exchange.properties.isUpdateSellableStock?? && exchange.properties.isUpdateSellableStock == true>
                 <SellableQuantity>${exchange.properties.overAllQuantity?string("0")}</SellableQuantity>
             <#else>
                 <Quantity>${exchange.properties.overAllQuantity?string("0")}</Quantity>
             </#if>
        </Sku>
<#else>
	<#list list as listObj>
    <Sku>
        <#if listObj.skuID?? && listObj.itemID??>
            <SkuId>${listObj.skuID?js_string}</SkuId>
            <ItemId>${listObj.itemID?js_string}</ItemId>
        </#if>
        <SellerSku>${listObj.sellerSKU}</SellerSku>
        <#if exchange.properties.isUpdateSellableStock?? && exchange.properties.isUpdateSellableStock == true>
            <SellableQuantity>${listObj.overAllQuantity?string("0")}</SellableQuantity>
        <#else>
            <Quantity>${listObj.overAllQuantity?string("0")}</Quantity>
        </#if>
    </Sku>
    </#list>
</#if>
</Skus>
</Product>
</Request>
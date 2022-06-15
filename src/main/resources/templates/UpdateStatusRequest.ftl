<#assign skuDataMap = exchange.properties.skuDataMap>
<#assign activeSellerSKUList = exchange.properties.activeSellerSKUList>
<#assign inactiveSellerSKUList = exchange.properties.inactiveSellerSKUList>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
     <Product>
          <#if exchange.properties.itemID??>
               <ItemId>${exchange.properties.itemID}</ItemId>
          </#if>
          <Skus>
               <#list skuDataMap?keys as key>
                    <#assign skuData = skuDataMap[key]>
                    <#assign customSKU = skuData.customSKU>
                    <Sku>
                         <SellerSku><![CDATA[${customSKU}]]></SellerSku>
                         <#if activeSellerSKUList?seq_contains(customSKU)>
                              <Status>active</Status>
                         <#elseif inactiveSellerSKUList?seq_contains(customSKU)>
                              <Status>inactive</Status>
                         </#if>
                    </Sku>
               </#list>
          </Skus>
     </Product>
</Request>
<#assign order = exchange.properties.order>
<#assign  orderItemsList = exchange.properties.orderItemList>
<#assign  shippingDetails = order.shippingDetails>
{
    "orderID" : "${order.orderID}",
    <#if exchange.properties.buyerDetails??>
        "buyerDetails" : ${exchange.properties.buyerDetails},
    </#if>
    <#if order.orderStatus??>
       "orderStatus": "${order.orderStatus}",
    </#if>
    <#if order.paymentStatus??>
       "paymentStatus": "${order.paymentStatus}",
    </#if>
    "shippingDetails" : ${shippingDetails},
    "orderSoldAmount" : ${exchange.properties.orderSoldAmount},
    "site" : ${constructSite(order)},
    "orderItems" : ${constructItems(orderItemsList)},
    <#if order.has("invoiceNumber")>
        "invoiceNumber": ${order.invoiceNumber},
    </#if>
    <#if order.has("dropOffAddressID")>
        "dropOffAddressID": ${order.dropOffAddressID},
    </#if>
    "timeOrderCreated": ${order.timeOrderCreated?c}
}

<#function constructSite order>
    <#assign site = "{\"nickNameID\":" +" \"${order.nickNameID}\""+",\"name\":" + "\"${order.site}\"}">
    <#return site>
</#function>

<#function constructItems orderItemsList>
    <#assign orderItems = "[">
        <#list orderItemsList as orderItem>
                <#assign orderItems = orderItems + "{\"itemTitle\":" + "\"${orderItem.itemTitle?js_string}\""
                +",\"orderItemID\":" + "\"${orderItem.orderItemID}\"" + ",\"quantity\":" + "${orderItem.quantity}">
                    <#if orderItem.variantDetails?? && orderItem.variantDetails ?has_content>
                        <#assign orderItems = orderItems +", \"variantDetails\":${constructVariantDetails(orderItem.variantDetails)}" >
                    </#if>
                    <#if orderItem.customSKU?? && orderItem.customSKU ?has_content>
                        <#assign orderItems = orderItems +", \"customSKU\":"+"\"${orderItem.customSKU?js_string}\"" >   
                    </#if>
                    <#if orderItem.SKU?? && orderItem.SKU ?has_content>
                        <#assign orderItems = orderItems +", \"SKU\":"+"\"${orderItem.SKU}\"" >
                    </#if>
                    <#if orderItem.wmsID?? && orderItem.wmsID ?has_content>
                        <#assign orderItems = orderItems +", \"wmsID\":"+"\"${orderItem.wmsID}\"" >
                    </#if>
                    <#if orderItem.hsnCode?? && orderItem.hsnCode?has_content>
                        <#assign orderItems = orderItems +", \"hsnCode\":"+"\"${orderItem.hsnCode}\"" >
                    </#if>
                    <#if orderItem.itemSoldAmount?? && orderItem.itemSoldAmount.amount??>
                        <#assign orderItems = orderItems + ", \"itemSoldAmount\" : {\"amount\":" + "${orderItem.itemSoldAmount.amount?c}"+",\"currencyCode\":" + "\"${orderItem.itemSoldAmount.currencyCode}\"}">
                    <#elseif orderItem.itemAmount?? && orderItem.itemAmount.amount??>
                        <#assign sellerDiscountAmount = 0>
                        <#if orderItem.sellerDiscountAmount?? && orderItem.sellerDiscountAmount.amount??>
                            <#assign sellerDiscountAmount = orderItem.sellerDiscountAmount.amount?c>
                        </#if>
                        <#assign orderItems = orderItems + ", \"itemSoldAmount\" : {\"amount\":" + "${(orderItem.itemAmount.amount?c?number - sellerDiscountAmount?number)?c}" + ",\"currencyCode\":" + "\"${orderItem.itemAmount.currencyCode}\"}">
                    </#if>
                    <#if orderItem?has_next>
                        <#assign orderItems = orderItems +"},">
                    <#else>
                        <#assign orderItems = orderItems +"}">
                    </#if>
        </#list>
        <#assign orderItems = orderItems +"]">
    <#return orderItems>
</#function>

<#function constructVariantDetails variantDetails>
	<#assign variantDetailsArray = "[">
	<#list variantDetails as variantDetail>
		<#assign variant = "{">
		<#if variantDetail.title?? && variantDetail.title ?has_content>
			<#assign variant = variant + "\"title\":"+"\"${variantDetail.title}\"">
	    </#if>
	    <#if variantDetail.name?? && variantDetail.name ?has_content>
	        <#assign variant = variant + ", \"name\":"+"\"${variantDetail.name}\"">
	    </#if>
	    <#assign variant = variant + "}">
	    <#assign variantDetailsArray = variantDetailsArray + variant>
	    <#if variantDetail?has_next>
			<#assign variantDetailsArray = variantDetailsArray + ",">
	    <#else>
			<#assign variantDetailsArray = variantDetailsArray + "]">
	    </#if>
	</#list>
	<#return variantDetailsArray>
</#function>
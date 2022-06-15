<#attempt>
<?xml version='1.0' encoding='UTF-8' ?>
<Request>
<Product>
<#if exchange.properties.inventory.lazada.itemID??>
	<ItemId>${exchange.properties.inventory.lazada.itemID?js_string}</ItemId>
</#if>
<PrimaryCategory>${exchange.properties.inventory.lazada.categoryID?js_string}</PrimaryCategory>
<#if exchange.properties.inventory.lazada.SPUId??>
	<PrimaryCategory>${exchange.properties.inventory.lazada.SPUId?js_string}</PrimaryCategory>
</#if>
<#if exchange.properties.newProductImageURI??>
	<Images>
		<#list exchange.properties.newProductImageURI as image>
			<Image>${image}</Image>
		</#list>
	</Images>
</#if>
<Attributes>
	<name><![CDATA[
		<#if exchange.properties.inventory.lazada.itemTitle??>
			${exchange.properties.inventory.lazada.itemTitle}
		<#else>
			${exchange.properties.inventory.itemTitle}
		</#if>]]>
	</name>
	<#if exchange.properties.inventory.lazada.itemTitleEnglish??>
		<name_en><![CDATA[${exchange.properties.inventory.lazada.itemTitleEnglish}]]></name_en>
	</#if>
	<description><![CDATA[
		<#if exchange.properties.itemDescription??>
			${exchange.properties.itemDescription}
		</#if>]]>
	</description>
	<short_description><![CDATA[
		<#if exchange.properties.shortDescription??>
			${exchange.properties.shortDescription}
		</#if>
		]]></short_description>
	<#if exchange.properties.shortDescriptionEnglish??>
		<short_description_en><![CDATA[${exchange.properties.shortDescriptionEnglish}]]></short_description_en>
	</#if>
	<#if exchange.properties.itemDescriptionEnglish??>
		<description_en><![CDATA[${exchange.properties.itemDescriptionEnglish}]]></description_en>
	</#if>
	<brand><![CDATA[${exchange.properties.inventory.lazada.brand}]]></brand>
	<#if exchange.properties.inventory.lazada.model??>
		<model><![CDATA[${exchange.properties.inventory.lazada.model}]]></model>
	</#if>
	<warranty_type><![CDATA[${exchange.properties.inventory.lazada.warrantyType}]]></warranty_type>
	<#if exchange.properties.inventory.lazada.itemSpecifics??>
		${processAttributes(exchange.properties.inventory.lazada.itemSpecifics)}
	</#if>
</Attributes>
<Skus>
<#assign inventoryList = []>
<#if exchange.properties.requestType == "batchEditItem">
     <#assign inventoryList = exchange.properties.inventoryVariantDetails>
<#elseif exchange.properties.inventoryDetails?? && exchange.properties.inventoryDetails?size != 0>
     <#assign inventoryList = exchange.properties.inventoryDetails>
</#if>
<#list inventoryList as inventory>
	<#if inventory.SKU?contains("-") || (!inventory.SKU?contains("-") && exchange.properties.hasVariants == false)>
		<#assign lazada = inventory.lazada>
		<Sku>
			<#if lazada.skuID??>
				<SkuId>${lazada.skuID?js_string}</SkuId>
			</#if>
			<#if inventory.customSKU??>
				<SellerSku><![CDATA[${inventory.customSKU}]]></SellerSku>
			<#else>
				<SellerSku><![CDATA[${inventory.SKU}]]></SellerSku>
			</#if>
			<#if lazada.noOfItem < 0 && exchange.properties.requestType == "batchEditItem">
			    <quantity>0</quantity>
			    <available>0</available>
			<#else>
			    <quantity>${lazada.noOfItem?string("0")}</quantity>
			    <available>${lazada.noOfItem?string("0")}</available>
			</#if>
			<price>${(lazada.itemAmount.amount/100)?string("0.00")}</price>
			<#if lazada.salePrice??>
				<special_price>${(lazada.salePrice.amount/100)?string("0.00")}</special_price>
				<#setting time_zone=exchange.properties.timeZoneOffset>
				<special_from_date>${(lazada.saleStartDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</special_from_date>
				<special_to_date>${(lazada.saleEndDate*1000)?number_to_datetime?string("yyyy-MM-dd HH:mm:ss")}</special_to_date>
			</#if>
			<#if lazada.newImageURI??>
				<Images>
					<#list lazada.newImageURI as image>
						<Image>${image}</Image>
					</#list>
				</Images>
			</#if>
			<package_height>${lazada.packageHeight}</package_height>
			<package_length>${lazada.packageLength}</package_length>
			<package_width>${lazada.packageWidth}</package_width>
			<package_weight>${lazada.packageWeight}</package_weight>
			<#if lazada.packageContent??>
			   <package_content><![CDATA[${lazada.packageContent}]]></package_content>
			<#else>
			   <package_content></package_content>
			</#if>
			<#if lazada.packageContentEnglish??>
			   <package_contents_en><![CDATA[${lazada.packageContentEnglish}]]></package_contents_en>
			</#if>
			<#if lazada.itemSpecifics??>
				${processSKUs(lazada.itemSpecifics)}
			</#if>
		</Sku>
	</#if>
</#list>
</Skus>
</Product>
</Request>
<#recover>
error
</#attempt>

<#function processAttributes itemSpecifics>
	<#assign attributes="">
	<#list itemSpecifics as itemSpecific>
		<#assign title = itemSpecific.title>
		<#if title?contains("normal.") && itemSpecific.names[0]!="" && !(exchange.properties.removeKeyPropFieldTitleList?? && exchange.properties.removeKeyPropFieldTitleList?contains(title?replace("normal.", "")?replace("[^a-zA-Z0-9]", "", "r")))>
			<#assign attributeName = title?replace("normal.", "")>
			<#assign attributes = attributes + "<"+attributeName+"><![CDATA[">
			<#assign attributes = attributes + itemSpecific.names[0]>
			<#assign attributes = attributes + "]]></"+attributeName+">">
		</#if>
	</#list>
	<#return attributes>
</#function>	

<#function processSKUs itemSpecifics>
	<#assign attributes="">
	<#list itemSpecifics as itemSpecific>
		<#assign title = itemSpecific.title>
		<#if title?contains("sku.") && itemSpecific.names[0]!="">
			<#assign attributeName = title?replace("sku.", "")>
			<#assign attributes = attributes + "<"+attributeName+"><![CDATA[">
			<#if attributeName == "std_search_keywords">
				<#assign attributes = attributes + "[" >
				<#list itemSpecific.names as name>
					<#assign attributes = attributes + "\"${name?js_string}\"" >
					<#if name?has_next>
						<#assign attributes = attributes + "," >
					</#if>
				</#list>
				<#assign attributes = attributes + "]" >
			<#else>
				<#assign attributes = attributes + itemSpecific.names[0]>
			</#if>
			<#assign attributes = attributes + "]]></"+attributeName+">">
		</#if>
	</#list>
	<#return attributes>
</#function>

	
	
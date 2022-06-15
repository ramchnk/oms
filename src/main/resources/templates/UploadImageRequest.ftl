<?xml version='1.0' encoding='UTF-8' ?>
<Request>
<#list exchange.properties.inventory.lazada.imageURI as image>
	<Image>
		<Url>
			${exchange.properties.inventory.imageURL?js_string}${image?js_string}
		</Url>
	</Image>
</#list>
</Request>
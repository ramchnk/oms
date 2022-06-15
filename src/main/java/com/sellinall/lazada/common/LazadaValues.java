package com.sellinall.lazada.common;

/**
 * @author Raju
 *
 */
public enum LazadaValues {
	SKU_FROM_CHANNEL ("Sku"),
	shopSKU_FROM_CHANNEL ("ShopSku"), 
	DROPSHIPPING ("Dropshipping"),
	SC_SERVER_DOWN("communication_failure");
	
	private final String name;       

	private LazadaValues(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}

}
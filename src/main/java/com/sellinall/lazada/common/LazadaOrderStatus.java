package com.sellinall.lazada.common;

public enum LazadaOrderStatus {
	PENDING ("pending"), /* The order has been placed but payment has not been authorized. The order is not ready for shipment. Note that for orders with OrderType = Standard, the initial order status is Pending. For orders with OrderType = Preorder (available in JP only), the initial order status is PendingAvailability, and the order passes into the Pending status when the payment authorization process begins. */
	REPACKED ("repacked"),
	PACKED ("packed"),
	UNPAID("unpaid"),
	UNVERIFIED("unverified"),
	SHIPPED("shipped"), /* All items in the order have been shipped */
	READYTOSHIP("ready_to_ship"),
	READYTOSHIPPENDING("ready_to_ship_pending"),
	DELIVERED("delivered"),
	RETURN("return"),
	RETURNED("returned"),
	FAILED("failed"),
	CANCELED("canceled"),
	BACK_TO_SHIPPER("INFO_ST_DOMESTIC_BACK_TO_SHIPPER"),
	DELIVERY_FAILED("INFO_ST_DOMESTIC_DELIVERY_FAILED"),
	PACKAGE_RETURNED("INFO_ST_DOMESTIC_PACKAGE_RETURNED"),
	RETURN_SHIPPED("INFO_ST_DOMESTIC_RETURN_AT_TRANSIT_HUB"),
	RETURN_SHIPPED_WITH_LAST_MILE("INFO_ST_DOMESTIC_RETURN_WITH_LAST_MILE_3PL"),
	LOST_BY_3PL("LOST_BY_3PL"),
	DAMAGE_BY_3PL("DAMAGE_BY_3PL"),
	PACKAGE_RETURN_FAILED("INFO_ST_DOMESTIC_PACKAGE_RETURNED_FAILED"),
	SHIPPER_RECEIVED("INFO_ST_DOMESTIC_SHIPPER_RECEIVED"),
	SCRAPPED("PACKAGE_SCRAPPED"),

	FAILED_DELIVERY("failed_delivery"),
	SHIPPED_BACK("shipped_back"),
	SHIPPED_BACK_SUCCESS("shipped_back_success"),
	SHIPPED_BACK_FAILED("shipped_back_failed"),
	PACKAGE_SCRAPPED("package_scrapped"),
	LOST_By_3PL("lost_by_3pl"),
	DAMAGED_BY_3PL("damaged_by_3pl");

	private final String name;       

	private LazadaOrderStatus(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}

}

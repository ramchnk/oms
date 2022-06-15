package com.sellinall.lazada.common;

public enum FlexiComboDiscountType {
	FIXED ("money"),
	PERCENTAGE ("discount"),
	FREEGIFT("freeGift"),
	FREESAMPLE ("freeSample"),
	PERCENTAGEANDFREEGIFT ("discountWithGift"),
	FIXEDANDFREEGIFT("moneyWithGift"),
	PERCENTAGEANDFREESAMPLE("discountWithSample"),
	FIXEDANDFREESAMPLE("moneyWithSample");
	private final String name;       

	private FlexiComboDiscountType(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}

}

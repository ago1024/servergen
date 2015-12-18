package org.gotti.wurmunlimited.servergen.pack;

public enum ServerDbs {

	CREATURES("wurmcreatures"),
	DEITIES("wurmdeities"),
	ECONOMY("wurmeconomy"),
	ITEMS("wurmitems"),
	LOGIN("wurmlogin"),
	LOGS("wurmlogs"),
	PLAYERS("wurmplayers"),
	TEMPLATES("wurmtemplates"),
	ZONES("wurmzones"),

	;

	private String dbName;

	private ServerDbs(String dbName) {
		this.dbName = dbName;
	}

	public String getDbName() {
		return dbName;
	}
}

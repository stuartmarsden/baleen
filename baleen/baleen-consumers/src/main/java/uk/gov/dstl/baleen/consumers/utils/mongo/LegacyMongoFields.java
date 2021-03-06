//Dstl (c) Crown Copyright 2015
package uk.gov.dstl.baleen.consumers.utils.mongo;

/** Entity converter fields for the legacy Mongo consumer.
 * 
 *
 */
public class LegacyMongoFields extends MongoFields {
	@Override
	public String getExternalId() {
		return "uniqueID";
	}
	
	@Override
	public String getHistory() {
		return null;
	}

	@Override
	public String getHistoryRecordable() {
		return null;
	}

	@Override
	public String getHistoryAction() {
		return null;
	}

	@Override
	public String getHistoryType() {
		return null;
	}

	@Override
	public String getHistoryParameters() {
		return null;
	}

	@Override
	public String getHistoryReferrer() {
		return null;
	}

	@Override
	public String getHistoryTimestamp() {
		return null;
	}
}

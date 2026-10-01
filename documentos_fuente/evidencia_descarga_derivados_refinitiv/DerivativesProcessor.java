package com.bbva.kytl.refinitivderivativesloader.processors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import com.bbva.kytl.refinitivderivativesloader.LoaderProcess;
import com.bbva.kytl.refinitivderivativesloader.entities.AuditFields;
import com.bbva.kytl.refinitivderivativesloader.entities.DataFields;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_FECH;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_FNCH;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_IEDF;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISCL;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISDE;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISGU;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISID;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISSU;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_MKIS;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_OPCH;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_RGCH;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_RIDF;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_RISS;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_SWCH;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_UWCH;
import com.bbva.kytl.refinitivderivativesloader.services.ExceptionService;
import com.bbva.kytl.refinitivderivativesloader.services.ListedDerivativesService;
import com.bbva.kytl.refinitivderivativesloader.utils.CfiDto;
import com.bbva.kytl.refinitivderivativesloader.utils.ExceptionsLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.GeographicDTO;
import com.bbva.kytl.refinitivderivativesloader.utils.MarketGuntDto;
import com.bbva.kytl.refinitivderivativesloader.utils.PositionsTemplate;
import com.bbva.kytl.refinitivderivativesloader.utils.StaticData;
import com.bbva.kytl.refinitivderivativesloader.utils.UtilsMethods;

public class DerivativesProcessor {

	private static Logger LOGGER_DERIVATIVES_PROCESSOR = Logger.getLogger(ListedDerivativesService.class);
	
	private static final String INACTIVE = "INACTIVE";
	
	private static final String ACTIVE = "ACTIVE";
	
	private static final String LAST_CHANGE_USER = "BBVA:CUSTOMER:REFINITIV:DERIVADOS";
	
	private static final String Y = "Y";
	
	private static final String N = "N";
	
	private static final String S = "S";
	
	private static final String NO_MARKET = "XXXX";
	
	private static final String QUOTE_PERM_ID = "QUOTE_PERM_ID";
	
	private static final String ISIN = "ISIN";
	
	private static final String BBGLOBAL = "BBGLOBAL";
	
	private static final String PUBLIC = "PUBLIC";
	
	private static final String REFINITIV = "REFINITIV";
	
	private static final String IN_HOUSE = "INHOUSE";
	
	private static final String PRIMARY = "PRIMARY";
	
	private static final String ENGLISH = "ENGLISH";
	
	private static final String  REGISTRATION = "REGSTRTN";
	
	private static final String  ISSUANCE = "ISSUANCE";
	
	private static final String COUNTRY = "COUNTRY";
	
	private static final String CFI = "CFI";
	
	private static final String OTHER = "OTHER";
	
	private static final String OPTIONS = "OPTIONS";
	
	private static final String R = "R";
	
	private static final String UNDLYING = "UNDLYING";
	
	private static final String ALL = "ALL";
	
	private static final String UNIT = "UNIT";
	
	private static final String EXCHANGE_CODE_REU = "REU";
	
	private static final String ACTION_DELETE = "D";
	
	private static final String UNKNOWN = "UNKNOWN"; 
	
	private static final String GUID_GB = "GB";
		
	private static final List<String> TYPE_OF_IDENTIFIERS = new ArrayList<>( Arrays.asList("QUOTE_PERM_ID","RIC", "RDR_ID", "ISIN", "ISSUE_PERM_ID", "TRDGSYMB", "BBGLOBAL", "TICKER_RFTNV", "TICKER"));
	
	private static final List<String> MARKET_LEVEL_IDENTIFIERS = new ArrayList<>( Arrays.asList("QUOTE_PERM_ID", "RIC", "TRDGSYMB", "BBGLOBAL"));
	
	private static final List<String> FAKE_UNDERLYINGS = new ArrayList<>( Arrays.asList("FUTRFV", "UNDLYRFV"));

	
	/**
	 * 
	 * @param issu
	 * @param data
	 * @param entityManager
	 */
	public static void updateDerivativeData(FT_T_ISSU issu, String[] data, EntityManager entityManager) {
		
		boolean entityHasBeenModified = false;
		
		boolean isFakeUnderlying = (FAKE_UNDERLYINGS.contains(issu.getIssTyp())) ? true : false;
		Date maturityDate = UtilsMethods.calculateMaturityDate(data[PositionsTemplate.EXPIRATION_DATE].replaceAll("-", ""), data[PositionsTemplate.LAST_TRADING_DATE].replaceAll("-", ""));
		String calculatedAltAssetClassTxt = (StaticData.getClassificationList().contains(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME])) ? data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME] : null ;
		String calculatedAssetCategory = (StaticData.getIssueSubTypeList().contains(data[PositionsTemplate.ASSET_CATEGORY])) ? data[PositionsTemplate.ASSET_CATEGORY] : null ;
		String securityDescription = data[PositionsTemplate.SECURITY_DESCRIPTION];
		String isin = (data[PositionsTemplate.ISIN].isEmpty()) ?  null : data[PositionsTemplate.ISIN];
		Date firstTradingDate = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.FIRST_TRADING_DATE].replaceAll("-", ""));
		Date lastDeliveryDate = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.LAST_DELIVERY_DATE].replaceAll("-", ""));
		String tradingStyle = (data[PositionsTemplate.TRADING_STYLE].isEmpty()) ? null : data[PositionsTemplate.TRADING_STYLE];
		String notionalCurrencyESMA = (data[PositionsTemplate.NOTIONAL_CURRENCY_ESMA].isEmpty()) ? null : data[PositionsTemplate.NOTIONAL_CURRENCY_ESMA];
		String notionalCurrency2ESMA = (data[PositionsTemplate.NOTIONAL_CURRENCY_2_ESMA].isEmpty()) ? null : data[PositionsTemplate.NOTIONAL_CURRENCY_2_ESMA];
		String mifidUnerlyingIndexName = (data[PositionsTemplate.MIFID_UNDERLYING_INDEX_NAME].isEmpty()) ? null : data[PositionsTemplate.MIFID_UNDERLYING_INDEX_NAME];
		Double strikePrice = (data[PositionsTemplate.STRIKE_PRICE].isEmpty()) ? null : Double.valueOf(data[PositionsTemplate.STRIKE_PRICE]);
		String currency = (data[PositionsTemplate.CURRENCY].isEmpty()) ? null : data[PositionsTemplate.CURRENCY];
		Double lotSize = (data[PositionsTemplate.LOT_SIZE].isEmpty()) ? null : Double.parseDouble(data[PositionsTemplate.LOT_SIZE]);
		String underlyingId = calculateUnderlyingId(data);
		String totvFlag = (data[PositionsTemplate.EEA_VENUE_ELIGIBLE_FLAG].isEmpty()) ? null : data[PositionsTemplate.EEA_VENUE_ELIGIBLE_FLAG]; 
		String mktOid = calculateMktOid(data[PositionsTemplate.MIC], data[PositionsTemplate.EXCHANGE_CODE]);
		List<String> prefIssData = calculatePrefIssId(findSpecificIdentifierInIssu(issu, BBGLOBAL), data);
		Date hofuriChgDte = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.HOFURI_CHG_DTE].replaceAll("-", ""));
		Date endTms = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.END_TMS].replaceAll("-", ""));
		
		
		if (!issu.getIssActvyStatTyp().trim().equals(StaticData.getStatusTranslationDataMap().get(data[PositionsTemplate.ASSET_STATUS])) ||
				ACTION_DELETE.equals(data[PositionsTemplate.ACTION])){
			LOGGER_DERIVATIVES_PROCESSOR.debug("Inactivating instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " )");
			issu.setIssActvyStatTyp(UtilsMethods.transalateRefinitivStatus(issu.getIssActvyStatTyp()));
			issu.getDataFields().setDataStatTyp(INACTIVE);
			issu.getDataFields().setDataSrcId(REFINITIV);
			issu.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
			issu.getAuditFields().setLastChgTms(new Date());
			for (FT_T_ISID isid : issu.getIsidList()) {
				isid.getDataFields().setDataStatTyp(INACTIVE);
				isid.getDataFields().setDataSrcId(REFINITIV);
				isid.getAuditFields().setLastChgTms(new Date());
				isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
			}
			
			entityHasBeenModified = true;
		} else {
			
			if (UtilsMethods.checkIfDatesAreDiferentes(issu.getMatExpTms() , maturityDate)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating maxExpTms of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");
				issu.setMatExpTms(maturityDate);
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(issu.getAltAssetClassTxt(), calculatedAltAssetClassTxt)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating AltAssetClassTxt of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");

				issu.setAltAssetClassTxt(calculatedAltAssetClassTxt);
				entityHasBeenModified = true;
				
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(issu.getIssTyp(), StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIssTyp())) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating issTyp, iscdOid and eistOid of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");

				
				issu.setIssTyp(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIssTyp());
				issu.setIscdOid(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIscdOid());
				issu.setEistOid(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getEistOid());
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(prefIssData.get(1), issu.getPrefIssId()) ||
					UtilsMethods.checkIfStringValuesAreDiferentes(prefIssData.get(0), issu.getPrefIdCtxtTyp())) {
				issu.setPrefIssId(prefIssData.get(1));
				issu.setPrefIdCtxtTyp(prefIssData.get(0));
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(data[PositionsTemplate.SECURITY_DESCRIPTION], issu.getPrefIssNme())) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating securityDescription of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");

				issu.setPrefIssNme(data[PositionsTemplate.SECURITY_DESCRIPTION]);
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfDoubleValuesAreDiferentes(issu.getPrcMltplrCrte(), lotSize)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating prcMltplrCrte of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");

				issu.setPrcMltplrCrte(lotSize);
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(issu.getClTyp(), calculatedAssetCategory)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating clTyp of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");
				issu.setClTyp(calculatedAssetCategory);
				entityHasBeenModified = true;
			}
			
			if (UtilsMethods.checkIfDatesAreDiferentes(issu.getIssTms(), firstTradingDate)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating issTms of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");
				issu.setIssTms(firstTradingDate);
				entityHasBeenModified = true;
				
			}
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(issu.getInstrIssrId(), StaticData.getIssuerDerivativeRelation().get(data[PositionsTemplate.ISSUER_ORG_ID]))) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating instrIssrId of instrument ( QuotePermId: "+ data[PositionsTemplate.QUOTE_PERM_ID] +" Instr_id: " + issu.getInstrId() + " ) ");
				issu.setInstrIssrId(StaticData.getIssuerDerivativeRelation().get(data[PositionsTemplate.ISSUER_ORG_ID]));
				entityHasBeenModified = true;
			}
			
			
			processIssueIdentifiers(issu, buildReceivedIdentifersMap(data, issu.getIssAlphSrchTxt()), mktOid, entityManager, isFakeUnderlying);
			
			processIssueDescription(issu, securityDescription, isin, entityManager);
			
			processIssueGeoUnit(issu, REGISTRATION, data[PositionsTemplate.COUNTRY_OF_INCORPORATION], null, data[PositionsTemplate.QUOTE_PERM_ID], entityManager);
			
			if (!(StaticData.getMarketGuntMapExchangeCode().get(data[PositionsTemplate.EXCHANGE_CODE]) != null && UNKNOWN.equals(StaticData.getMarketGuntMapExchangeCode().get(data[PositionsTemplate.EXCHANGE_CODE]).getGuId()))) {
				processIssueGeoUnit(issu, ISSUANCE, null, data[PositionsTemplate.EXCHANGE_CODE], data[PositionsTemplate.QUOTE_PERM_ID], entityManager);
			}
			
			processCfiCode(issu, data[PositionsTemplate.CFI_CODE].toUpperCase(), data[PositionsTemplate.QUOTE_PERM_ID], entityManager);

			processLastDeliveryDate(issu, tradingStyle, lastDeliveryDate, entityManager);
			
			processMethodOfDelivery(issu, StaticData.getMethodOfDeliveryTranslationMap().get(data[PositionsTemplate.METHOD_OF_DELIVERY]), entityManager);
			
			processSwapCharacteristics(issu, notionalCurrencyESMA, notionalCurrency2ESMA, data[PositionsTemplate.QUOTE_PERM_ID], entityManager);

			processFundCharacteristics(issu, mifidUnerlyingIndexName, entityManager);
			
			processOptionsCharacteristics(issu, data[PositionsTemplate.QUOTE_PERM_ID], data[PositionsTemplate.PUT_CALL_INDICATOR], strikePrice, currency, data[PositionsTemplate.EXERCISE_STYLE], entityManager);
			
			processMarketIssueCharacteristics(issu, data[PositionsTemplate.CURRENCY], firstTradingDate, mktOid, entityManager);
			
			processRelatedIssue(issu, underlyingId, data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME], lotSize, data[PositionsTemplate.PUT_CALL_INDICATOR], entityManager);
			
			processTotv(issu, totvFlag, data[PositionsTemplate.MIC], data[PositionsTemplate.EXCHANGE_CODE], entityManager, hofuriChgDte, endTms);
		}
		
		if (entityHasBeenModified) {
			issu.getAuditFields().setLastChgTms(new Date());
			issu.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
		}
		
		entityManager.persist(issu);
		
	}
	
	/**
	 * 
	 * @param data
	 * @param entityManager
	 */
	public static void insertDerivativeData(String[] data, EntityManager entityManager) {
		Date maturityDate = UtilsMethods.calculateMaturityDate(data[PositionsTemplate.EXPIRATION_DATE].replaceAll("-", ""), data[PositionsTemplate.LAST_TRADING_DATE].replaceAll("-", ""));
		String calculatedAltAssetClassTxt = (StaticData.getClassificationList().contains(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME])) ? data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME] : null ;
		String calculatedAssetCategory = (StaticData.getIssueSubTypeList().contains(data[PositionsTemplate.ASSET_CATEGORY])) ? data[PositionsTemplate.ASSET_CATEGORY] : null ;
		Date firstTradingDate = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.FIRST_TRADING_DATE].replaceAll("-", ""));
		String securityDescription = data[PositionsTemplate.SECURITY_DESCRIPTION];
		String isin = (data[PositionsTemplate.ISIN].isEmpty()) ?  null : data[PositionsTemplate.ISIN];
		Date lastDeliveryDate = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.LAST_DELIVERY_DATE].replaceAll("-", ""));
		String tradingStyle = (data[PositionsTemplate.TRADING_STYLE].isEmpty()) ? null : data[PositionsTemplate.TRADING_STYLE]; 
		String notionalCurrencyESMA = (data[PositionsTemplate.NOTIONAL_CURRENCY_ESMA].isEmpty()) ? null : data[PositionsTemplate.NOTIONAL_CURRENCY_ESMA];
		String notionalCurrency2ESMA = (data[PositionsTemplate.NOTIONAL_CURRENCY_2_ESMA].isEmpty()) ? null : data[PositionsTemplate.NOTIONAL_CURRENCY_2_ESMA];
		String mifidUnerlyingIndexName = (data[PositionsTemplate.MIFID_UNDERLYING_INDEX_NAME].isEmpty()) ? null : data[PositionsTemplate.MIFID_UNDERLYING_INDEX_NAME];
		Double strikePrice = (data[PositionsTemplate.STRIKE_PRICE].isEmpty()) ? null : Double.valueOf(data[PositionsTemplate.STRIKE_PRICE]);
		String currency = (data[PositionsTemplate.CURRENCY].isEmpty()) ? null : data[PositionsTemplate.CURRENCY];
		Double lotSize = (data[PositionsTemplate.LOT_SIZE].isEmpty()) ? null : Double.parseDouble(data[PositionsTemplate.LOT_SIZE]);
		String underlyingId = calculateUnderlyingId(data);
		String totvFlag = (data[PositionsTemplate.EEA_VENUE_ELIGIBLE_FLAG].isEmpty()) ? null : data[PositionsTemplate.EEA_VENUE_ELIGIBLE_FLAG];
		String mktOid = calculateMktOid(data[PositionsTemplate.MIC], data[PositionsTemplate.EXCHANGE_CODE]);
		List<String> prefIssData = calculatePrefIssId(null, data); 
		Date hofuriChgDte = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.HOFURI_CHG_DTE].replaceAll("-", ""));
		Date endTms = UtilsMethods.formateDateFromRefinitiv(data[PositionsTemplate.END_TMS].replaceAll("-", ""));
				
		AuditFields auditFields = new AuditFields();
		
		auditFields.setLastChgTms(new Date());
		auditFields.setStartTms(new Date());
		auditFields.setLastChgUsrId(LAST_CHANGE_USER);
		
		DataFields dataFields = new DataFields();
		dataFields.setDataSrcId(REFINITIV);
		dataFields.setDataStatTyp(ACTIVE);
		
		FT_T_ISSU issu = new FT_T_ISSU (entityManager);
		
		issu.setIssActvyStatTyp(ACTIVE);
		issu.setMatExpTms(maturityDate);
		issu.setPrefIdCtxtTyp(prefIssData.get(0));
		issu.setPrefIssDesc(data[PositionsTemplate.QUOTE_PERM_ID]);
		issu.setAltAssetClassTxt(calculatedAltAssetClassTxt);
		issu.setIssTyp(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIssTyp());
		issu.setIscdOid(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIscdOid());
		issu.setEistOid(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getEistOid());
		issu.setPrefIssId(prefIssData.get(1));
		issu.setPrefIssNme(data[PositionsTemplate.SECURITY_DESCRIPTION]);
		issu.setPrcMltplrCrte(lotSize);
		issu.setClTyp(calculatedAssetCategory);
		issu.setIssTms(firstTradingDate);
		issu.setAccessAuthTyp(PUBLIC);
		issu.setIssAlphSrchTxt(UtilsMethods.createRDRId(entityManager));
		issu.setTrdgRstTyp(S);
		issu.setInstrIssrId(StaticData.getIssuerDerivativeRelation().get(data[PositionsTemplate.ISSUER_ORG_ID]));
		
		issu.setAuditFields(auditFields);
		issu.setDataFields(dataFields);
		
		processIssueIdentifiers(issu, buildReceivedIdentifersMap(data, issu.getIssAlphSrchTxt()), mktOid, entityManager, false);
		
		processIssueDescription(issu, securityDescription, isin, entityManager);
		
		processIssueGeoUnit(issu, REGISTRATION, data[PositionsTemplate.COUNTRY_OF_INCORPORATION], null, data[PositionsTemplate.QUOTE_PERM_ID], entityManager);
		
		if (!(StaticData.getMarketGuntMapExchangeCode().get(data[PositionsTemplate.EXCHANGE_CODE]) != null && UNKNOWN.equals(StaticData.getMarketGuntMapExchangeCode().get(data[PositionsTemplate.EXCHANGE_CODE]).getGuId()))) {
			processIssueGeoUnit(issu, ISSUANCE, null, data[PositionsTemplate.EXCHANGE_CODE], data[PositionsTemplate.QUOTE_PERM_ID], entityManager);
		}
		
		
		processCfiCode(issu, data[PositionsTemplate.CFI_CODE].toUpperCase(), data[PositionsTemplate.QUOTE_PERM_ID], entityManager);

		processLastDeliveryDate(issu, tradingStyle, lastDeliveryDate, entityManager);
		
		processMethodOfDelivery(issu, StaticData.getMethodOfDeliveryTranslationMap().get(data[PositionsTemplate.METHOD_OF_DELIVERY]), entityManager);
		
		processSwapCharacteristics(issu, notionalCurrencyESMA, notionalCurrency2ESMA, data[PositionsTemplate.QUOTE_PERM_ID],entityManager);
		
		processFundCharacteristics(issu, mifidUnerlyingIndexName, entityManager);
		
		setIncomeEventDefinition(issu, entityManager);
		
		processOptionsCharacteristics(issu, data[PositionsTemplate.QUOTE_PERM_ID], data[PositionsTemplate.PUT_CALL_INDICATOR], strikePrice, currency, data[PositionsTemplate.EXERCISE_STYLE], entityManager);
		
		processMarketIssueCharacteristics(issu, data[PositionsTemplate.CURRENCY], firstTradingDate, mktOid, entityManager);
		
		processRelatedIssue(issu, underlyingId, data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME], lotSize, data[PositionsTemplate.PUT_CALL_INDICATOR], entityManager);
		
		processTotv(issu, totvFlag, data[PositionsTemplate.MIC], data[PositionsTemplate.EXCHANGE_CODE], entityManager, hofuriChgDte, endTms);
		
		entityManager.persist(issu);
		
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	private static List<String> calculatePrefIssId(String bbglobal, String[] data) {
		
		if (data[PositionsTemplate.ISIN] != null && !data[PositionsTemplate.ISIN].isEmpty()) {
			return new ArrayList<>( Arrays.asList(ISIN, data[PositionsTemplate.ISIN]));
			
		} else if (data[PositionsTemplate.FIGI] != null && !data[PositionsTemplate.FIGI].isEmpty()) {
			return new ArrayList<>( Arrays.asList(BBGLOBAL, data[PositionsTemplate.FIGI]));
			
		} else if (bbglobal != null && !bbglobal.isEmpty()) {
			return new ArrayList<>( Arrays.asList(BBGLOBAL, bbglobal));
			
		} else {
			return new ArrayList<>( Arrays.asList(QUOTE_PERM_ID, data[PositionsTemplate.QUOTE_PERM_ID]));

		}
	
	}

	/**
	 * 
	 * @param issu
	 * @param identifiersMap
	 * @param mic
	 * @param exchangeCode
	 * @param entityManager
	 */
	private static void processIssueIdentifiers(FT_T_ISSU issu, HashMap<String, String> identifiersMap, String mktOid, EntityManager entityManager, boolean isFakeUnderlying) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing issue identifiers of instrument (INSTR_ID: " + issu.getInstrId()+")");
		
		List<FT_T_ISID> identifiersToAdd = new ArrayList<FT_T_ISID>();
		
		String mergeUniqOid = "";
		
		if ((LoaderProcess.onlineExecution || isFakeUnderlying) && ExceptionService.vreqOid == null) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Creating  QuotePermId identifier of instrument (INSTR_ID: " + issu.getInstrId()+")");
			FT_T_ISID isid = new FT_T_ISID(entityManager);
			mergeUniqOid = isid.getIsidOid();
			
			AuditFields auditFields =  new AuditFields();
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			DataFields dataFields = new DataFields();
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			isid.setIdCtxtTyp(QUOTE_PERM_ID);
			isid.setIssId(identifiersMap.get(QUOTE_PERM_ID));
			isid.setInstrId(issu.getInstrId());
			isid.setInstrSymbolStatTyp(ACTIVE);
			isid.setGlobalUniqInd(Y);
			isid.setMktOid(mktOid);
			isid.setDataFields(dataFields);
			isid.setAuditFields(auditFields);
			
			issu.getIsidList().add(isid);
		}
		
		for (String identifier : TYPE_OF_IDENTIFIERS) {
				
			boolean identifierExists = false;
			
			if (issu.getIsidList() != null) {
				
				for (FT_T_ISID isid : issu.getIsidList()) {
					
					if (mergeUniqOid == null || mergeUniqOid.isEmpty()) {
						
						if (QUOTE_PERM_ID.equals(isid.getIdCtxtTyp())) {
							mergeUniqOid = isid.getIsidOid();
						} else {
							mergeUniqOid = isid.getMergeUniqOid();
						}
						
					}
					
					if (identifier.equals(isid.getIdCtxtTyp())) {
						LOGGER_DERIVATIVES_PROCESSOR.debug("Updating "+ isid.getIdCtxtTyp() + "identifier of instrument (INSTR_ID: " + issu.getInstrId()+")");
						identifierExists = true;
						if (MARKET_LEVEL_IDENTIFIERS.contains(identifier) && (isid.getMktOid() == null || !isid.getMktOid().equals(mktOid))) {
							isid.setMktOid(mktOid);
							isid.getAuditFields().setLastChgTms(new Date());
							isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						}
						
						if (!identifier.equals("RDR_ID") && !identifier.equals("QUOTE_PERM_ID") && identifiersMap.get(identifier) != null && !isid.getIssId().equals(identifiersMap.get(identifier))){
							isid.setIssId(identifiersMap.get(identifier));
							isid.getAuditFields().setLastChgTms(new Date());
							isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						}
						
						if ((!identifier.equals("RDR_ID") && !identifier.equals("TICKER") && !identifier.equals("TICKER") &&  !identifier.equals("BBGLOBAL")&& identifiersMap.get(identifier) == null)
								|| (identifier.equals("TICKER") && LAST_CHANGE_USER.equals(isid.getAuditFields().getLastChgUsrId()) && identifiersMap.get(identifier) == null)
								|| (identifier.equals("BBGLOBAL") && LAST_CHANGE_USER.equals(isid.getAuditFields().getLastChgUsrId()) && identifiersMap.get(identifier) == null)
								) {
							isid.getAuditFields().setLastChgTms(new Date());
							isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
							isid.getDataFields().setDataStatTyp(INACTIVE);
							isid.getDataFields().setDataSrcId(REFINITIV);
						}
						
						break;
					}
					
					if (isid.getMergeUniqOid() == null && !QUOTE_PERM_ID.equals(isid.getIdCtxtTyp())) {
						isid.setMergeUniqOid(mergeUniqOid);
						isid.getAuditFields().setLastChgTms(new Date());
						isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					}
					
				}
			}
			
			if (!identifierExists && identifiersMap.get(identifier) != null) {
				
				LOGGER_DERIVATIVES_PROCESSOR.debug("Creating "+ identifier + "identifier of instrument (INSTR_ID: " + issu.getInstrId()+")");
				FT_T_ISID isid = new FT_T_ISID(entityManager);
				
				AuditFields auditFields =  new AuditFields();
				auditFields.setLastChgTms(new Date());
				auditFields.setLastChgUsrId(LAST_CHANGE_USER);
				auditFields.setStartTms(new Date());
				
				DataFields dataFields = new DataFields();
				dataFields.setDataSrcId(REFINITIV);
				dataFields.setDataStatTyp(ACTIVE);
				
				isid.setIdCtxtTyp(identifier);
				isid.setIssId(identifiersMap.get(identifier));
				isid.setInstrId(issu.getInstrId());
				isid.setInstrSymbolStatTyp(ACTIVE);
				
				if (mergeUniqOid.isEmpty()) {
					mergeUniqOid = isid.getIsidOid();
				}
				
				if (!identifier.equals(QUOTE_PERM_ID)) {
					isid.setMergeUniqOid(mergeUniqOid);
					isid.setGlobalUniqInd(N);
					
				} else {
					isid.setGlobalUniqInd(Y);
					
				}
				
				if (MARKET_LEVEL_IDENTIFIERS.contains(identifier)) {
					isid.setMktOid(mktOid);
				}
				
				isid.setDataFields(dataFields);
				isid.setAuditFields(auditFields);
				
				identifiersToAdd.add(isid);
			}
			
		}
		if (issu.getIsidList() != null) {
			issu.getIsidList().addAll(identifiersToAdd);
		} else {
			issu.setIsidList(identifiersToAdd);
		}
		
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param securityDescription
	 * @param isin
	 * @param entityManager
	 */
	private static void processIssueDescription(FT_T_ISSU issu, String securityDescription, String isin, EntityManager entityManager) {
		
		
		if (issu.getIsdeList() != null && !issu.getIsdeList().isEmpty()) {
			boolean isdeHasBeenModified;
			LOGGER_DERIVATIVES_PROCESSOR.debug("Updating issue description of instrument (Instr_id: " + issu.getInstrId() + " )" );
			for (FT_T_ISDE isde : issu.getIsdeList()) {
				isdeHasBeenModified = false;
				if (!isde.getIssNme().equals(securityDescription)) {
					isde.setIssNme(securityDescription);
					isdeHasBeenModified=true;
				} 
				
				if (UtilsMethods.checkIfStringValuesAreDiferentes(isde.getIssDesc(), isin)) {
					isde.setIssDesc(isin);
					isdeHasBeenModified = true;
				}
				
				if (isdeHasBeenModified) {
					isde.getAuditFields().setLastChgTms(new Date());
					isde.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					
				}
				
			}
			
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Creating issue description of instrument (Instr_id: " + issu.getInstrId() + " )" );
			FT_T_ISDE isde =  new FT_T_ISDE(entityManager);
			
			AuditFields auditFields = new AuditFields();
			auditFields.setLastChgTms(new Date());
			auditFields.setStartTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			DataFields dataFields = new DataFields();
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			isde.setDescSrceTyp(IN_HOUSE);
			isde.setIssNme(securityDescription);
			isde.setIssDesc(isin);
			isde.setDescUsageTyp(PRIMARY);
			isde.setNlsCde(ENGLISH);
			isde.setInstrId(issu.getInstrId());
			isde.setAuditFields(auditFields);
			isde.setDataFields(dataFields);
			
			issu.setIsdeList(new ArrayList<FT_T_ISDE>( Arrays. asList(isde)));
			
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param blockType
	 * @param countryOfIncorporation
	 */
	private static void processIssueGeoUnit(FT_T_ISSU issu, String blockType, String countryOfIncorporation, String exchangeCode,String quotePermId, EntityManager entityManager) {
		
		List<FT_T_ISGU>  isguList ;
		Object geographicData; 
		
		String guId = null;
		Integer guCnt = null;
		String guntOid = null;
		
		if (REGISTRATION.equals(blockType)) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Processing geographic Registration information of instrument (Instr_id: " + issu.getInstrId() + " )" );
			geographicData = StaticData.getGeographicDataMap().get(countryOfIncorporation);
			isguList = issu.getIsguListRegistration();
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Processing geographic Issuance information of instrument (Instr_id: " + issu.getInstrId() + " )" );
			geographicData = StaticData.getMarketGuntMapExchangeCode().get(exchangeCode);
			isguList = issu.getIsguListIssuance();
		}
		
		if (geographicData != null) {
		
			if (REGISTRATION.equals(blockType)) {
				guId = ((GeographicDTO) geographicData).getGuId();
				guCnt = ((GeographicDTO) geographicData).getGuCnt();
				guntOid = ((GeographicDTO) geographicData).getGuntOid();
			} else if (ISSUANCE.equals(blockType) ) {
				guId = ((MarketGuntDto) geographicData).getGuId();
				guCnt = ((MarketGuntDto) geographicData).getGuCnt();
				guntOid = ((MarketGuntDto) geographicData).getGuntOid();
			} 
		} else {
			
			if (REGISTRATION.equals(blockType) && !countryOfIncorporation.isEmpty() && ExceptionService.vreqOid == null) {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("countryOfRegistration", countryOfIncorporation);
				parameterErrorMap.put("quotePermId", quotePermId);
				ExceptionService.createException(entityManager, ExceptionsLibrary.COUNTRY_OF_REGISTRATION_DOES_NOT_EXISTS_IN_RDR, parameterErrorMap);
				LOGGER_DERIVATIVES_PROCESSOR.debug("Country of Registration of instrument (QuotePermId: " + quotePermId + "; Instr_id: " + issu.getInstrId() + " ) ) does not exist in RDR" );
			} else if (ISSUANCE.equals(blockType) && !exchangeCode.isEmpty() && !EXCHANGE_CODE_REU.equals(exchangeCode) && ExceptionService.vreqOid == null) {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("exchangeCode", exchangeCode);
				parameterErrorMap.put("quotePermId", quotePermId);
				ExceptionService.createException(entityManager, ExceptionsLibrary.WARNING_EXCHANGE_CODE_DOES_NOT_EXISTS_IN_RDR, parameterErrorMap);
				LOGGER_DERIVATIVES_PROCESSOR.debug("Country of Issuance of instrument (QuotePermId: " + quotePermId + "; Instr_id: " + issu.getInstrId() + " ) does not exist in RDR" );
			}
				
		}
		
		if ((isguList == null || isguList.isEmpty())  && guId != null && guCnt != null && guntOid != null) {
			
			isguList = new ArrayList<FT_T_ISGU>();
			
			FT_T_ISGU isgu = new FT_T_ISGU(entityManager);
			
			AuditFields auditFields = new AuditFields();
			auditFields.setStartTms(new Date());
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			
			DataFields dataFields =  new DataFields();
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			isgu.setIssGuPurpTyp(blockType);
			isgu.setGuTyp(COUNTRY);
			
			isgu.setGuId(guId);
			isgu.setGuCnt(guCnt);
			isgu.setGuntOid(guntOid);
			isgu.setAuditFields(auditFields);
			isgu.setDataFields(dataFields);
			isgu.setInstrId(issu.getInstrId());
			isguList.add(isgu);
			
			if (REGISTRATION.equals(blockType)) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Insert Registration country for instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");
				issu.setIsguListRegistration(isguList);
			} else {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Insert Issuance country for instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");
				issu.setIsguListIssuance(isguList);
			}
		} else if (isguList != null && !isguList.isEmpty()){
			LOGGER_DERIVATIVES_PROCESSOR.debug("Updateing " + blockType + " country for instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");

			for (FT_T_ISGU isgu : isguList) {
				
				if (!isgu.getGuId().equals(guId) && guId != null) {
					isgu.setGuId(guId);
					isgu.setGuCnt(guCnt);
					isgu.setGuntOid(guntOid);
					isgu.getAuditFields().setLastChgTms(new Date());
					isgu.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					isgu.getDataFields().setDataSrcId(REFINITIV);
					
				} else if (guId == null) {
					isgu.getAuditFields().setLastChgTms(new Date());
					isgu.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					isgu.getDataFields().setDataSrcId(REFINITIV);
					isgu.getDataFields().setDataStatTyp(INACTIVE);
				}
					
				
			}
			
		}
		
	}

	/**
	 * 
	 * @param issu
	 * @param cfiCode
	 * @param quotePermId
	 * @param entityManager
	 */
	private static void processCfiCode(FT_T_ISSU issu, String cfiCode, String quotePermId, EntityManager entityManager) {
		
		if (!cfiCode.isEmpty()) {
			CfiDto cfiDto;
			LOGGER_DERIVATIVES_PROCESSOR.debug("Processing CFI Code of instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");
			if ((cfiDto= StaticData.getCfiMap().get(cfiCode))!= null) {
				
				if (issu.getIsclList() != null && !issu.getIsclList().isEmpty()) {
					
					for (FT_T_ISCL iscl : issu.getIsclList()) {
						LOGGER_DERIVATIVES_PROCESSOR.debug("Updating CFI Code of instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");
						if (!iscl.getClsfOid().equals(cfiDto.getClsfOid()) || 
							!iscl.getIndusClSetId().equals(cfiDto.getIndusClSetId()) ||
							!iscl.getClValue().equals(cfiDto.getClValue())) {
							
							iscl.setClsfOid(cfiDto.getClsfOid());
							iscl.setIndusClSetId(cfiDto.getIndusClSetId());
							iscl.setClValue(cfiDto.getClValue());
							iscl.getAuditFields().setLastChgTms(new Date());
							iscl.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
							iscl.getDataFields().setDataSrcId(REFINITIV);
							
						}
					}	
					
				} else {
					LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting CFI Code of instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " )");

					FT_T_ISCL iscl = new FT_T_ISCL(entityManager);
					
					AuditFields auditFields =  new AuditFields();
					auditFields.setLastChgTms(new Date());
					auditFields.setLastChgUsrId(LAST_CHANGE_USER);
					auditFields.setStartTms(new Date());
					
					DataFields dataFields = new DataFields();
					dataFields.setDataSrcId(REFINITIV);
					dataFields.setDataStatTyp(ACTIVE);
					
					iscl.setIndusClSetId(cfiDto.getIndusClSetId());
					iscl.setClsfOid(cfiDto.getClsfOid());
					iscl.setClValue(cfiDto.getClValue());
					iscl.setClsfPurpTyp(CFI);
					iscl.setInstrId(issu.getInstrId());
					iscl.setAuditFields(auditFields);
					iscl.setDataFields(dataFields);
					issu.setIsclList(new ArrayList<FT_T_ISCL>( Arrays.asList(iscl)));
				}
				
			} else {
				if (issu.getIsclList() != null && !issu.getIsclList().isEmpty()) {
					LOGGER_DERIVATIVES_PROCESSOR.debug("Inactivating CFI Code of instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " ) because CFI Code recieved from Refinitiv does not exist in RDR");

					for (FT_T_ISCL iscl : issu.getIsclList()) {
					
						iscl.getAuditFields().setLastChgTms(new Date());
						iscl.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						
						iscl.getDataFields().setDataSrcId(REFINITIV);
						iscl.getDataFields().setDataStatTyp(INACTIVE);

					}	
					
				}
				
				if (ExceptionService.vreqOid == null) {
					HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
					parameterErrorMap.put("cfiCode", cfiCode);
					parameterErrorMap.put("quotePermId", quotePermId);
					ExceptionService.createException(entityManager, ExceptionsLibrary.CFI_CODE_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
				}
				
			}
			
		} else {
			if (issu.getIsclList() != null) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Inactivating CFI Code of instrument ( QuotePermId: " + quotePermId + " Instr_id: " + issu.getInstrId() + " ) beacuse CFI Code was not reported from Refinitiv");
				for (FT_T_ISCL iscl : issu.getIsclList()) {
				
					iscl.getAuditFields().setLastChgTms(new Date());
					iscl.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					iscl.getDataFields().setDataSrcId(REFINITIV);
					iscl.getDataFields().setDataStatTyp(INACTIVE);
					
				}
			}	
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param tradingStyle
	 * @param lastDeliveryDate
	 * @param entityManager
	 */
	private static void processLastDeliveryDate(FT_T_ISSU issu, String tradingStyle, Date lastDeliveryDate, EntityManager entityManager) {
	
		if (issu.getFechList() != null && !issu.getFechList().isEmpty()) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Updating last delivery date of instrument (  Instr_id: " + issu.getInstrId() + " )");

			for (FT_T_FECH fech : issu.getFechList()) {
				
				if (UtilsMethods.checkIfDatesAreDiferentes(fech.getLastDlvDte(), lastDeliveryDate)||
					UtilsMethods.checkIfStringValuesAreDiferentes(fech.getPrcngSessionTyp(), tradingStyle)) {
				
					fech.setPrcngSessionTyp(tradingStyle);
					fech.setLastDlvDte(lastDeliveryDate);
					fech.getAuditFields().setLastChgTms(new Date());
					fech.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
				
				}
			}
			
		} else {
			FT_T_FECH fech = new FT_T_FECH(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			fech.setPrcngSessionTyp(tradingStyle);
			fech.setLastDlvDte(lastDeliveryDate);
			fech.setAuditFields(auditFields);
			fech.setDataFields(dataFields);
			fech.setInstrId(issu.getInstrId());
			issu.setFechList(new ArrayList<FT_T_FECH>( Arrays.asList(fech)));
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param methodOfDelivery
	 * @param entityManager
	 */
	private static void processMethodOfDelivery(FT_T_ISSU issu, String methodOfDelivery, EntityManager entityManager) {
		
		if (issu.getUwchList() != null && !issu.getUwchList().isEmpty()) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Updating method of delivery of instrument (  Instr_id: " + issu.getInstrId() + " )");
			for (FT_T_UWCH uwch : issu.getUwchList()) {
				
				if (!methodOfDelivery.equals(uwch.getDlvTyp())) {
					
					uwch.setDlvTyp(methodOfDelivery);
					uwch.getAuditFields().setLastChgTms(new Date());
					uwch.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					uwch.getDataFields().setDataSrcId(REFINITIV);
				}
				
			}
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting method of delivery of instrument (  Instr_id: " + issu.getInstrId() + " )");
			FT_T_UWCH uwch = new FT_T_UWCH(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			uwch.setDlvTyp(methodOfDelivery);
			uwch.setOfferNum("1");
			uwch.setAuditFields(auditFields);
			uwch.setDataFields(dataFields);
			uwch.setInstrId(issu.getInstrId());
			issu.setUwchList(new ArrayList<FT_T_UWCH>( Arrays.asList(uwch)));
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param notionalCurrencyESMA
	 * @param notionalCurrency2ESMA
	 * @param entityManager
	 */
	private static void processSwapCharacteristics(FT_T_ISSU issu, String notionalCurrencyESMA, String notionalCurrency2ESMA, String quotePermId,EntityManager entityManager) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing Swap characteristic of instrument (  Instr_id: " + issu.getInstrId() + " )");
		
		if (notionalCurrencyESMA  != null && !StaticData.getCurrencyList().contains(notionalCurrencyESMA) && ExceptionService.vreqOid == null) {
			HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
			parameterErrorMap.put("currencyCode", notionalCurrencyESMA);
			parameterErrorMap.put("quotePermId", quotePermId);
			ExceptionService.createException(entityManager, ExceptionsLibrary.NOTIONAL_CURRENCY_CODE_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
			notionalCurrencyESMA = null;
			LOGGER_DERIVATIVES_PROCESSOR.error ("notional CUrrency ESMA of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");
		}
		if (notionalCurrency2ESMA  != null && !StaticData.getCurrencyList().contains(notionalCurrency2ESMA) && ExceptionService.vreqOid == null) {
			HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
			parameterErrorMap.put("currencyCode", notionalCurrency2ESMA);
			parameterErrorMap.put("quotePermId", quotePermId);
			ExceptionService.createException(entityManager, ExceptionsLibrary.NOTIONAL_CURRENCY_2_CODE_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
			notionalCurrency2ESMA= null;
			LOGGER_DERIVATIVES_PROCESSOR.error ("notional CUrrency ESMA 2 of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");
		}
		
		if (issu.getSwchList() != null && !issu.getSwchList().isEmpty()) {
			
			for (FT_T_SWCH swch : issu.getSwchList()) {
				
				if (UtilsMethods.checkIfStringValuesAreDiferentes(swch.getSwapNotlCurrCde(), notionalCurrencyESMA)||
					UtilsMethods.checkIfStringValuesAreDiferentes(swch.getSwapNotl2CurrCde(), notionalCurrency2ESMA)) {
					
					LOGGER_DERIVATIVES_PROCESSOR.debug ("Updating Swap Characteristics of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");
					
					swch.setSwapNotlCurrCde(notionalCurrencyESMA);
					swch.setSwapNotl2CurrCde(notionalCurrency2ESMA);
					swch.getAuditFields().setLastChgTms(new Date());
					swch.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					swch.getDataFields().setDataSrcId(REFINITIV);
					
				}
				
			}
			
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug ("Inserting Swap Characteristics of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");
			
			FT_T_SWCH swch = new FT_T_SWCH(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			swch.setSwapNotlCurrCde(notionalCurrencyESMA);	
			swch.setSwapNotl2CurrCde(notionalCurrency2ESMA);
			swch.setAuditFields(auditFields);
			swch.setDataFields(dataFields);
			swch.setInstrId(issu.getInstrId());
			issu.setSwchList(new ArrayList<FT_T_SWCH>( Arrays.asList(swch)));
			
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param quotePermId
	 * @param putCallIndicator
	 * @param strikePrice
	 * @param strikePriceCurrencyESMA
	 * @param exerciseStyle
	 * @param entityManager
	 */
	private static void processOptionsCharacteristics(FT_T_ISSU issu, String quotePermId, String putCallIndicator, Double strikePrice, String strikePriceCurrencyESMA, String exerciseStyle, EntityManager entityManager) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing Options Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");
		
		if (strikePriceCurrencyESMA != null && !StaticData.getCurrencyList().contains(strikePriceCurrencyESMA) && ExceptionService.vreqOid == null) { 
			HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
			parameterErrorMap.put("strikeCurrency", strikePriceCurrencyESMA);
			parameterErrorMap.put("quotePermId", quotePermId);
			ExceptionService.createException(entityManager, ExceptionsLibrary.STRIKE_CURRENCY_CODE_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
			strikePriceCurrencyESMA = null;
			LOGGER_DERIVATIVES_PROCESSOR.error ("Strike price Currency ESMA of instrument (  Instr_id: " + issu.getInstrId() + " ) does not exist in RDR");

		}
		
		
		if (OPTIONS.equals(issu.getIssTyp())) {
			
			if (issu.getOpchList() != null && !issu.getOpchList().isEmpty()) {
				
				for (FT_T_OPCH opch : issu.getOpchList()) {
					
					if (UtilsMethods.checkIfStringValuesAreDiferentes(opch.getCallPutTyp(), putCallIndicator) ||
							UtilsMethods.checkIfDoubleValuesAreDiferentes(opch.getStrkeCprc(), strikePrice) ||
							UtilsMethods.checkIfStringValuesAreDiferentes(opch.getStrkeprcCurrCde(), strikePriceCurrencyESMA) ||
							UtilsMethods.checkIfStringValuesAreDiferentes(opch.getExerTyp(), exerciseStyle) ) {
						
						LOGGER_DERIVATIVES_PROCESSOR.debug("Updating Options Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " )");

						
						opch.setCallPutTyp(putCallIndicator);
						opch.setStrkeCprc(strikePrice);
						opch.setStrkeprcCurrCde(strikePriceCurrencyESMA);
						opch.setExerTyp(exerciseStyle);
						
						opch.getAuditFields().setLastChgTms(new Date());
						opch.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						
						opch.getDataFields().setDataSrcId(REFINITIV);
					}
					
				}
				
			} else {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting Options Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
				FT_T_OPCH opch = new FT_T_OPCH(entityManager);
				AuditFields auditFields = new AuditFields();
				DataFields dataFields = new DataFields();
				
				auditFields.setLastChgTms(new Date());
				auditFields.setLastChgUsrId(LAST_CHANGE_USER);
				auditFields.setStartTms(new Date());
				
				dataFields.setDataSrcId(REFINITIV);
				dataFields.setDataStatTyp(ACTIVE);
				
				opch.setCallPutTyp(putCallIndicator);
				opch.setStrkeCprc(strikePrice);
				opch.setStrkeprcCurrCde(strikePriceCurrencyESMA);
				
				opch.setExerTyp(exerciseStyle);
				opch.setInstrId(issu.getInstrId());
				opch.setDataFields(dataFields);
				opch.setAuditFields(auditFields);
				
				issu.setOpchList(new ArrayList<FT_T_OPCH>( Arrays.asList(opch)));
			}
			
		}
	}
	
	/**
	 * 
	 * @param issu
	 * @param mifidUnerlyingIndexName
	 * @param entityManager
	 */
	private static void processFundCharacteristics(FT_T_ISSU issu, String mifidUnerlyingIndexName, EntityManager entityManager) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing funds Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
		if (issu.getFnchList() != null && !issu.getFnchList().isEmpty()) {
			
			for (FT_T_FNCH fnch : issu.getFnchList()) {
				
				if (fnch.getUnderlyIndexId() != null && !fnch.getUnderlyIndexId().equals(mifidUnerlyingIndexName)) {
					LOGGER_DERIVATIVES_PROCESSOR.debug("Updating fund Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
					fnch.setUnderlyIndexId(mifidUnerlyingIndexName);
					fnch.getAuditFields().setLastChgTms(new Date());
					fnch.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					fnch.getDataFields().setDataSrcId(REFINITIV);
				}
			} 
			
			
		} else {
			
			if (mifidUnerlyingIndexName != null) {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting fund Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
				FT_T_FNCH fnch = new FT_T_FNCH(entityManager);
				AuditFields auditFields = new AuditFields();
				DataFields dataFields = new DataFields();
				
				auditFields.setLastChgTms(new Date());
				auditFields.setLastChgUsrId(LAST_CHANGE_USER);
				auditFields.setStartTms(new Date());
				
				dataFields.setDataSrcId(REFINITIV);
				dataFields.setDataStatTyp(ACTIVE);
				
				fnch.setUnderlyIndexId(mifidUnerlyingIndexName);
				fnch.setAuditFields(auditFields);
				fnch.setDataFields(dataFields);
				fnch.setInstrId(issu.getInstrId());
				issu.setFnchList(new ArrayList<FT_T_FNCH>( Arrays.asList(fnch)));
			}
		}
	
	}
	
	/**
	 * 
	 * @param issu
	 * @param entityManager
	 */
	private static void setIncomeEventDefinition(FT_T_ISSU issu, EntityManager entityManager) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting Income event Definition of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
		
		FT_T_IEDF iedf = new FT_T_IEDF(entityManager);
		AuditFields auditFields = new AuditFields();
		DataFields dataFields = new DataFields();
		
		auditFields.setLastChgTms(new Date());
		auditFields.setLastChgUsrId(LAST_CHANGE_USER);
		auditFields.setStartTms(new Date());
		
		dataFields.setDataSrcId(REFINITIV);
		dataFields.setDataStatTyp(ACTIVE);
		
		iedf.setEvTyp(OTHER);
		iedf.setRndMethTyp(R);
		iedf.setVerifInd(Y);
		iedf.setInstrId(issu.getInstrId());
		iedf.setAuditFields(auditFields);
		iedf.setDataFields(dataFields);
		
		issu.setIedfList(new ArrayList<FT_T_IEDF>( Arrays.asList(iedf)));
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param currencyCode
	 * @param firstTradingDate
	 * @param mktOid
	 * @param entityManager
	 */
	private static void processMarketIssueCharacteristics(FT_T_ISSU issu, String currencyCode, Date firstTradingDate, String mktOid, EntityManager entityManager) {
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing market issue Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
		if (issu.getMkisList() != null && !issu.getMkisList().isEmpty()) {
			
			for (FT_T_MKIS mkis : issu.getMkisList()) {
				
				if (UtilsMethods.checkIfStringValuesAreDiferentes(mkis.getPrcCurrCde(), currencyCode) ||
					UtilsMethods.checkIfStringValuesAreDiferentes(mkis.getTrdngCurrCde(), currencyCode) ||
					UtilsMethods.checkIfDatesAreDiferentes(mkis.getFirstTrdngTms(), firstTradingDate) ||
					UtilsMethods.checkIfStringValuesAreDiferentes(mkis.getMktOid(), mktOid)){
						LOGGER_DERIVATIVES_PROCESSOR.debug("Updating market issue Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
						mkis.setPrcCurrCde(currencyCode);
						mkis.setTrdngCurrCde(currencyCode);
						mkis.setFirstTrdngTms(firstTradingDate);
						mkis.setMktOid(mktOid);
						mkis.getAuditFields().setLastChgTms(new Date());
						mkis.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
					
				}
				
			}
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting market issue Characteristic of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
			FT_T_MKIS mkis = new FT_T_MKIS(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			mkis.setPrcCurrCde(currencyCode);
			mkis.setTrdngCurrCde(currencyCode);
			mkis.setFirstTrdngTms(firstTradingDate);
			mkis.setMktOid(mktOid);
			mkis.setInstrId(issu.getInstrId());
			mkis.setTrdngStatTyp(ACTIVE);
			mkis.setPrimTrdMktInd(Y);
			mkis.setAuditFields(auditFields);
			mkis.setDataFields(dataFields);
			
			issu.setMkisList(new ArrayList<FT_T_MKIS>( Arrays.asList(mkis)));
			
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param underlyingId
	 * @param refinitivClassificationScheme
	 * @param lotSize
	 * @param putCallIndicator
	 * @param entityManager
	 */
	private static void processRelatedIssue(FT_T_ISSU issu, String underlyingId, String refinitivClassificationScheme,Double lotSize, String putCallIndicator, EntityManager entityManager) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing related Issue of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
		
		String underlyingCurrencyCode = null;
		
		FT_T_RIDF ridf = null;
		
		String underlyingInstrId = StaticData.getUnderlyingInstrIdMap().get(underlyingId);
		
		if (underlyingId != null) {
			underlyingCurrencyCode = StaticData.getUnderlyingCurrencyCodeMap().get(underlyingId);
		}
		
		String relTyp = StaticData.getIssueTypeMap().get(refinitivClassificationScheme).getIssTyp();
		
		if (issu.getRidfList() != null && !issu.getRidfList().isEmpty()) {
			
			ridf = issu.getRidfList().get(0);
			
			if (UtilsMethods.checkIfDoubleValuesAreDiferentes(ridf.getAcltCntrctSizeCamt(), lotSize) ||
					UtilsMethods.checkIfStringValuesAreDiferentes(ridf.getCallPutTyp(), putCallIndicator) || 
					UtilsMethods.checkIfStringValuesAreDiferentes(ridf.getUnderlyCurrCde(), underlyingCurrencyCode) ||
					UtilsMethods.checkIfStringValuesAreDiferentes(ridf.getRelTyp(), relTyp) ){
				
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating Underlying Issue (Underlying InstrId : " + underlyingInstrId +" ; Underlying Id : " + underlyingId+ " ) of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
				
				ridf.setAcltCntrctSizeCamt(lotSize);
				ridf.setCallPutTyp(putCallIndicator);
				ridf.setUnderlyCurrCde(underlyingCurrencyCode);
				ridf.setRelTyp(relTyp);
				
				ridf.getAuditFields().setLastChgTms(new Date());
				ridf.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
				
				ridf.getDataFields().setDataSrcId(REFINITIV);
				
			}
			
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting Underlying Issue (Underlying InstrId : " + underlyingInstrId +" ; Underlying RIC : " + underlyingId+ " ) of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
			ridf = new FT_T_RIDF(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			ridf.setAcltCntrctSizeCamt(lotSize);
			ridf.setCallPutTyp(putCallIndicator);
			ridf.setUnderlyCurrCde(underlyingCurrencyCode);
			ridf.setRelTyp(relTyp);
			ridf.setAuditFields(auditFields);
			ridf.setDataFields(dataFields);
			
			ridf.setInstrId(issu.getInstrId());
			
			issu.setRidfList(new ArrayList<FT_T_RIDF>( Arrays.asList(ridf)));
			
		}
				
		if (ridf.getRissList() != null && !ridf.getRissList().isEmpty()) {
			
			FT_T_RISS riss = ridf.getRissList().get(0);
			
			if (UtilsMethods.checkIfStringValuesAreDiferentes(underlyingInstrId, riss.getInstrId())) {
				
				LOGGER_DERIVATIVES_PROCESSOR.debug("Updating RISS entity of instrument (  Instr_id: " + issu.getInstrId() + " ) ");

				riss.setInstrId(underlyingInstrId);
				riss.getAuditFields().setLastChgTms(new Date());
				riss.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
				riss.getDataFields().setDataSrcId(REFINITIV);
				
			}
			
		} else {
			
			LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting RISS entity of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
			
			FT_T_RISS riss = new FT_T_RISS(entityManager);
			AuditFields auditFields = new AuditFields();
			DataFields dataFields = new DataFields();
			
			auditFields.setLastChgTms(new Date());
			auditFields.setLastChgUsrId(LAST_CHANGE_USER);
			auditFields.setStartTms(new Date());
			
			dataFields.setDataSrcId(REFINITIV);
			dataFields.setDataStatTyp(ACTIVE);
			
			riss.setIssPartRlTyp(UNDLYING);
			riss.setPartUnitsTyp(ALL);
			riss.setComponentInstrCqty(Double.valueOf(1));
			riss.setInstrId(underlyingInstrId);
			riss.setRldIssFeatId(ridf.getRldIssFeatId());
			riss.setEvAmtTyp(UNIT);
			riss.setEvBasPrtCamt(Double.valueOf(1));
			
			riss.setAuditFields(auditFields);
			riss.setDataFields(dataFields);
			
			ridf.setRissList(new ArrayList<FT_T_RISS>( Arrays.asList(riss)));
		}
		
	}
	
	/**
	 * 
	 * @param issu
	 * @param totvIndicator
	 * @param mic
	 * @param exchangeCode
	 * @param entityManager
	 */
	private static void processTotv(FT_T_ISSU issu, String totvIndicator, String mic, String exchangeCode, EntityManager entityManager, Date hofuriChgDte, Date endTms) {
		
		LOGGER_DERIVATIVES_PROCESSOR.debug("Processing ToTV of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
		
		MarketGuntDto geographicData; 
		
		if (mic != null && !mic.isEmpty()) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Geographic Data of instrument (  Instr_id: " + issu.getInstrId() + " ) was calculated by MIC");
			geographicData = StaticData.getMarketGuntMapMic().get(mic);
		} else if (exchangeCode != null & !exchangeCode.isEmpty() && !EXCHANGE_CODE_REU.equals(exchangeCode)) {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Geographic Data of instrument (  Instr_id: " + issu.getInstrId() + " ) was calculated by ExchangeCode");
			geographicData = StaticData.getMarketGuntMapExchangeCode().get(exchangeCode);
		} else {
			LOGGER_DERIVATIVES_PROCESSOR.debug("Geographic Data of instrument (  Instr_id: " + issu.getInstrId() + " ) was calculated by NO MARKET");
			geographicData = StaticData.getMarketGuntMapMic().get(NO_MARKET);
		}
		
		if (geographicData != null && geographicData.getPrntGuId() != null && geographicData.getPrntGuCnt() != null
				&& geographicData.getPrntGuTyp() != null && geographicData.getGuntOid() != null) {
			String guId = geographicData.getPrntGuId();
			Integer guCnt = geographicData.getPrntGuCnt();
			String guTyp= geographicData.getPrntGuTyp();
			String guOid = geographicData.getGuntOid();
			
			if (issu.getRgchList() != null && !issu.getRgchList().isEmpty()) {
				boolean itWasFound = false;
				for (FT_T_RGCH rgch : issu.getRgchList()) {
					
					if (guId != null && guId.equals(rgch.getGuId()) && 
							guCnt == rgch.getGuCnt() && 
							guTyp != null && guTyp.equals(rgch.getGuTyp()) && 
							guOid != null && guOid.equals(rgch.getGuntOid())) {
						itWasFound = true;
						boolean itWasChanged = false;
						
						if (totvIndicator != null && UtilsMethods.checkIfStringValuesAreDiferentes(rgch.getMifidRegulatedInd(), totvIndicator)) {
							LOGGER_DERIVATIVES_PROCESSOR.debug("Updating ToTV of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
							rgch.setMifidRegulatedInd(totvIndicator);
							itWasChanged = true;
						}
						
						if (hofuriChgDte!=null && UtilsMethods.checkIfDatesAreDiferentes(rgch.getHofuriChgDte(), hofuriChgDte) && !guId.equals(GUID_GB)) {
							rgch.setHofuriChgDte(hofuriChgDte);
							itWasChanged = true;
						}
						
						if (endTms !=null && UtilsMethods.checkIfDatesAreDiferentes(rgch.getAuditFields().getEndTms(), endTms) && !guId.equals(GUID_GB)) {
							rgch.getAuditFields().setEndTms(endTms);
							itWasChanged = true;
						}
						
						if (itWasChanged) {
							rgch.getAuditFields().setLastChgTms(new Date());
							rgch.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
							rgch.getDataFields().setDataSrcId(REFINITIV);
						}
						
						
					}
						
				}
				
				if (!itWasFound) {
					LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting ToTV of instrument (  Instr_id: " + issu.getInstrId() + " ) to RGCH existing list");
					FT_T_RGCH rgch =  new FT_T_RGCH(entityManager);
					AuditFields auditFields = new AuditFields();
					DataFields dataFields = new DataFields();
					
					auditFields.setLastChgTms(new Date());
					auditFields.setLastChgUsrId(LAST_CHANGE_USER);
					auditFields.setStartTms(new Date());
					
					dataFields.setDataSrcId(REFINITIV);
					dataFields.setDataStatTyp(ACTIVE);
					
					rgch.setInstrId(issu.getInstrId());
					rgch.setGuCnt(guCnt);
					rgch.setGuId(guId);
					rgch.setGuTyp(guTyp);
					rgch.setGuntOid(guOid);
					rgch.setMifidRegulatedInd(totvIndicator);
					rgch.setAuditFields(auditFields);
					rgch.setDataFields(dataFields);
					
					if (hofuriChgDte != null && !guId.equals(GUID_GB)) {
						rgch.setHofuriChgDte(hofuriChgDte);
					}
					
					if (endTms != null && !guId.equals(GUID_GB)) {
						rgch.getAuditFields().setEndTms(endTms);
					}
				
					issu.getRgchList().add(rgch);
				}
			
			} else {
				LOGGER_DERIVATIVES_PROCESSOR.debug("Inserting ToTV of instrument (  Instr_id: " + issu.getInstrId() + " ) ");
				FT_T_RGCH rgch =  new FT_T_RGCH(entityManager);
				AuditFields auditFields = new AuditFields();
				DataFields dataFields = new DataFields();
				
				auditFields.setLastChgTms(new Date());
				auditFields.setLastChgUsrId(LAST_CHANGE_USER);
				auditFields.setStartTms(new Date());
				
				dataFields.setDataSrcId(REFINITIV);
				dataFields.setDataStatTyp(ACTIVE);
				
				rgch.setInstrId(issu.getInstrId());
				rgch.setGuCnt(guCnt);
				rgch.setGuId(guId);
				rgch.setGuTyp(guTyp);
				rgch.setGuntOid(guOid);
				rgch.setMifidRegulatedInd(totvIndicator);
				rgch.setAuditFields(auditFields);
				rgch.setDataFields(dataFields);
				
				if (hofuriChgDte != null && !guId.equals(GUID_GB)) {
					rgch.setHofuriChgDte(hofuriChgDte);
				}
				
				if (endTms != null && !guId.equals(GUID_GB)) {
					rgch.getAuditFields().setEndTms(endTms);
				}
				
				issu.setRgchList(new ArrayList<FT_T_RGCH>( Arrays.asList(rgch)));
			}
		
		}
	}
	
	
	/**
	 * 
	 * @param data
	 * @param cannonicIdentifier
	 * @return
	 */
	private static HashMap<String, String> buildReceivedIdentifersMap(String[] data, String cannonicIdentifier) {
		
		HashMap<String, String> identifiersMap = new HashMap<String, String>();
		
		identifiersMap.put("QUOTE_PERM_ID", data[PositionsTemplate.QUOTE_PERM_ID]);
		
		if (data[PositionsTemplate.ISIN] != null && !data[PositionsTemplate.ISIN].isEmpty()) {
			identifiersMap.put("ISIN", data[PositionsTemplate.ISIN]);
		}
		
		if (data[PositionsTemplate.RIC] != null && !data[PositionsTemplate.RIC].isEmpty()) {
			identifiersMap.put("RIC", data[PositionsTemplate.RIC]);
		}
		if (data[PositionsTemplate.ISSUE_PERM_ID] != null && !data[PositionsTemplate.ISSUE_PERM_ID].isEmpty()) {
			identifiersMap.put("ISSUE_PERM_ID", data[PositionsTemplate.ISSUE_PERM_ID]);
		}
		
		if (data[PositionsTemplate.TRADING_SYMBOL] != null && !data[PositionsTemplate.TRADING_SYMBOL].isEmpty()) {
			identifiersMap.put("TRDGSYMB", data[PositionsTemplate.TRADING_SYMBOL]);
		}
		
		identifiersMap.put("RDR_ID", cannonicIdentifier);
		
		if (data[PositionsTemplate.FIGI] != null && !data[PositionsTemplate.FIGI].isEmpty()) {
			identifiersMap.put("BBGLOBAL", data[PositionsTemplate.FIGI]);
		}
		
		if (data[PositionsTemplate.TICKER_RFTNV] != null && !data[PositionsTemplate.TICKER_RFTNV].isEmpty()) {
			identifiersMap.put("TICKER_RFTNV", data[PositionsTemplate.TICKER_RFTNV]);
		}
		
		if (data[PositionsTemplate.TICKER] != null && !data[PositionsTemplate.TICKER].isEmpty()) {
			identifiersMap.put("TICKER", data[PositionsTemplate.TICKER]);
		}
		
		return identifiersMap;
	}
	
	/**
	 * 
	 * @param mic
	 * @param exchangeCode
	 * @return
	 */
	private static String calculateMktOid(String mic, String exchangeCode) {
		
		if (mic != null && !mic.isEmpty()) {
			return (StaticData.getMarketGuntMapMic().get(mic).getMktOid() != null) ? StaticData.getMarketGuntMapMic().get(mic).getMktOid() : NO_MARKET;
		}
		
		if (exchangeCode!= null && !exchangeCode.isEmpty() && !EXCHANGE_CODE_REU.equals(exchangeCode)) {
			return (StaticData.getMarketGuntMapExchangeCode().get(exchangeCode).getMktOid() != null) ? StaticData.getMarketGuntMapExchangeCode().get(exchangeCode).getMktOid() : NO_MARKET;
		}
		
		return StaticData.getMarketGuntMapMic().get(NO_MARKET).getMktOid();
		
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	private static String calculateUnderlyingId (String[] data) {
		
		if (data[PositionsTemplate.UNDERLYING_RIC] != null && !data[PositionsTemplate.UNDERLYING_RIC].isEmpty()) {
			return data[PositionsTemplate.UNDERLYING_RIC];
		} else if (data[PositionsTemplate.UNDERLYING_ISIN] != null && !data[PositionsTemplate.UNDERLYING_ISIN].isEmpty()){
			return data[PositionsTemplate.UNDERLYING_ISIN];
		} else if (data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN] != null && !data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN].isEmpty()){
			return data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN];
		} else{
			return data[PositionsTemplate.UNDERLYING_ISIN_ESMA];
		}
		
	}
	
	private static String findSpecificIdentifierInIssu(FT_T_ISSU issu, String identifier) {
		
		String idValue = null;
		for (FT_T_ISID isid : issu.getIsidList()) {
			
			if (identifier.equals(isid.getIdCtxtTyp()) && !LAST_CHANGE_USER.equals(isid.getAuditFields().getLastChgUsrId())) {
				idValue = isid.getIssId();
				break;
			}
			
		}
		return idValue;
		
	}
}

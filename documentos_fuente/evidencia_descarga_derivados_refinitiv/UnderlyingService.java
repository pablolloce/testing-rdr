package com.bbva.kytl.refinitivderivativesloader.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import com.bbva.kytl.refinitivderivativesloader.LoaderProcess;
import com.bbva.kytl.refinitivderivativesloader.entities.AuditFields;
import com.bbva.kytl.refinitivderivativesloader.entities.DataFields;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISID;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISSU;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_MKIS;
import com.bbva.kytl.refinitivderivativesloader.utils.ExceptionsLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.QueryLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.StaticData;
import com.bbva.kytl.refinitivderivativesloader.utils.UnderlyingPositionsTemplate;
import com.bbva.kytl.refinitivderivativesloader.utils.UtilsMethods;

/**
 * Load process of the underlyings received from Refinitiv
 * @author Nfq
 *
 */
public class UnderlyingService {

	/** LOGGER class*/
	private static Logger LOGGER_ISSUER_MAIN = Logger.getLogger(UnderlyingService.class);
	
	/** Max number of threads used in this process*/
	private static final int maxNumberOfThreads = 20;
	
	/**
	 * Method used to load the underlyings in the file
	 * @param underlyingFile Underlyings received from Refinitiv
	 * @return true if the load was successfully and false if not 
	 */
	public static boolean loadUnderlyings(File underlyingFile) {
		
		final ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxNumberOfThreads);
		
		try {
			LOGGER_ISSUER_MAIN.debug("Reading file");
			BufferedReader csvReader = new BufferedReader(new FileReader(underlyingFile));
			ArrayList<String> listLines = new ArrayList<String>();
			
			boolean last = false;
			String line;
			String nextLine =csvReader.readLine();
			
			while (!last) {
				line = nextLine;
				last =(nextLine = csvReader.readLine()) == null;
				listLines.add(line);
				
				if (listLines.size() == 500 || last) {
					poolExecutor.execute(new UnderlyingThread((List<String>) listLines.clone()));
					listLines.clear();
				}
				
			}
			csvReader.close();
			poolExecutor.shutdown();
			try {
				poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				LOGGER_ISSUER_MAIN.error("Process out of time: ", e);
				return false;
			}
			
			
		} catch (FileNotFoundException e) {
			
			LOGGER_ISSUER_MAIN.error("Issuer file could no be found: " , e);
			return false;
		} catch (IOException e) {
			LOGGER_ISSUER_MAIN.error("Error reading file: ", e);
		}
		
		return true;
	}
	
	/**
	 * Thread class used for the Underlyings load
	 * @author Nfq
	 *
	 */
	private static class UnderlyingThread implements Runnable {

		/** RIC of the underlying processed*/
		private List<String> underlyingsDataList;
		
		/** EntityManager for persist*/
		private EntityManager entityManager;
		
		/** RIC constant*/
		private static final String RIC = "RIC";
		
		/** ACTIVE constant*/
		private static final String ACTIVE = "ACTIVE";
		
		/** REFINITIV constant*/
		private static final String REFINITIV = "REFINITIV";
		
		/** UNDLYRFV constant*/
		private static final String UNDERLYING_REFINITIV = "UNDLYRFV";
		
		private static final String UNDERLYING_FUTURE_REFINITIV = "FUTRFV";
		
		/** Y constant*/
		private static final String GLOBAL_UNIQ_IND = "Y";
		
		/** BBVA:CUSTOMER:REFINITIV:DERIVADOS constat*/
		private static final String LAST_CHANGE_USER = "BBVA:CUSTOMER:REFINITIV:DERIVADOS";
		
		/** Identifiers to process*/
		private static List<String> TYPE_OF_IDENTIFIERS;
		
		/** RDR Canonical identifier*/
		private static final String RDR_ID = "RDR_ID";

		/**Character that shows if the underlying is inactive*/
		private static final String UNDERLYING_INACTIVE_CHARACETER = "^";
		
		private static final String EQINDEX = "EQINDEX";
		
		/** Logger class of the Thread*/
		private static Logger LOGGER_UNDERLYING_THREAD = Logger.getLogger(UnderlyingThread.class);
		
		/** Constructor of the class*/
		private UnderlyingThread(List<String> listLines) {
			this.underlyingsDataList = listLines;
		}
		
		@Override
		public void run() {
			
			entityManager = LoaderProcess.entityManagerFactory.createEntityManager();
			TypedQuery<FT_T_ISSU> issuQuery = null;
			boolean isDuplicated;
			for (String underlyingData: this.underlyingsDataList) {
				isDuplicated = false;
				if (underlyingData != null && !underlyingData.isEmpty()) {
					String[] splittedData = underlyingData.split("\\|");
					
					if (!splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID].isEmpty() && 
							!splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID].contains(UNDERLYING_INACTIVE_CHARACETER)) {
						
						issuQuery =	 entityManager.createQuery(QueryLibrary.GET_ISSU_QUERY, FT_T_ISSU.class);
						issuQuery.setParameter("underlyingId", splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID]);
						issuQuery.setParameter("underlyingIdType", splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID_TYPE]);
						List<FT_T_ISSU> issuResponseList = issuQuery.getResultList(); 
						
						if (issuResponseList != null && !issuResponseList.isEmpty()) {
							
							int hashCode = issuResponseList.get(0).hashCode();
							for (FT_T_ISSU issu : issuResponseList) {
								
								if (hashCode !=issu.hashCode()) {
									isDuplicated = true;
									StaticData.addDuplicatedUnderlying(splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID]);
									if (ExceptionService.vreqOid == null) {
										LOGGER_UNDERLYING_THREAD.debug("The underlying with " + splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID_TYPE] + ": " + splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID] + " is duplicated in RDR");
										HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
										parameterErrorMap.put("underlyingId", splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID]);
										parameterErrorMap.put("underIdType", splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID_TYPE]);
										ExceptionService.createException(entityManager, ExceptionsLibrary.UNDERLYING_DUPLICATED_EXCEPTION, parameterErrorMap);
									}
									break;
								}
								
							}
							
							if (!isDuplicated) {
								treatResponse(issuResponseList, splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID], splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID_TYPE], splittedData[UnderlyingPositionsTemplate.UNDERLYING_TYPOLOGY]);
							}
						} else {
							treatResponse(issuResponseList, splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID], splittedData[UnderlyingPositionsTemplate.UNDERLYING_ID_TYPE], splittedData[UnderlyingPositionsTemplate.UNDERLYING_TYPOLOGY]);
						}
						
					}
				}
				
				
			}
			
			entityManager.getTransaction().begin();
			entityManager.getTransaction().commit();
			entityManager.close();
			
		}
		
		/**
		 * Method used to treat DDBB responsesni
		 * 
		 * @param issuResponseEntity DDBB response entity 
		 */
		private void treatResponse(List<FT_T_ISSU> issuResponseEntityList, String underlyingId, String underlyingIdType, String underlyingTypology) {
			
			FT_T_ISSU issuResponseEntity = null;
			
			
			if (!issuResponseEntityList.isEmpty() && (issuResponseEntity = issuResponseEntityList.get(0))!= null && issuResponseEntity.getIsidList().size() > 1 ) {
				
				StaticData.addRelationUnderlyingInstrIdMap(underlyingId, issuResponseEntity.getInstrId());
				
				if (ExceptionService.vreqOid == null) {
					HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
					parameterErrorMap.put("underIdType", underlyingIdType);
					parameterErrorMap.put("underlyingId", underlyingId);
					
					ExceptionService.createException(entityManager, ExceptionsLibrary.UNDERLYING_IS_DUPLICATED_IN_SAME_INSTRUMENT, parameterErrorMap);
				}
				
			} else {
				
				if (issuResponseEntity != null && issuResponseEntity.getIsidList().size() == 1) {
					LOGGER_UNDERLYING_THREAD.debug("The underlying with " + underlyingIdType + " : " + underlyingId + " already exists in RDR");
					
					StaticData.addRelationUnderlyingInstrIdMap(underlyingId, issuResponseEntity.getInstrId());
					
					String mktOid = null;
					
					if (RIC.equals(underlyingIdType)) {
						mktOid = calculateMktOid(issuResponseEntity.getUnderlyingRICisidList(), underlyingId);
					}
					
					
					if (RIC.equals(underlyingIdType) && mktOid!= null) {
						TypedQuery<FT_T_MKIS> mkisQuery = entityManager.createQuery(QueryLibrary.GET_UNDERLYING_CURRENCY, FT_T_MKIS.class);
						mkisQuery.setParameter("issu", issuResponseEntity);
						mkisQuery.setParameter("mktOid", mktOid);
						List<FT_T_MKIS> mkisResponseList = mkisQuery.getResultList();
						
						if (mkisResponseList != null && mkisResponseList.size() == 1) {
							loadUnderlyingMap(mkisResponseList.get(0).getPrcCurrCde(), underlyingId, entityManager);
						} else if (mkisResponseList != null && mkisResponseList.size() > 1 && ExceptionService.vreqOid == null) {
							HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
							parameterErrorMap.put("underlyingric", underlyingId);
							ExceptionService.createException(entityManager, ExceptionsLibrary.UNDERLYING_RIC_MARKET_ASSOCIATION_IS_DUPLICATED, parameterErrorMap);
						} else if (!EQINDEX.equals(issuResponseEntity.getIssTyp()) && ExceptionService.vreqOid == null){
							HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
							parameterErrorMap.put("underlyingric", underlyingId);
							ExceptionService.createException(entityManager, ExceptionsLibrary.UNDERLYING_RIC_DOES_NOT_HAVE_THE_ASSOCIATION_IN_MKIS, parameterErrorMap);
						}
						
					} else if (RIC.equals(underlyingIdType) && 
								!UNDERLYING_REFINITIV.equals(issuResponseEntity.getIssTyp()) && 
								!UNDERLYING_FUTURE_REFINITIV.equals(issuResponseEntity.getIssTyp()) &&
								!EQINDEX.equals(issuResponseEntity.getIssTyp()) &&
								ExceptionService.vreqOid == null) {
							HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
							parameterErrorMap.put("underlyingric", underlyingId);
							ExceptionService.createException(entityManager, ExceptionsLibrary.UNDERLYING_RIC_DOES_NOT_HAVE_MKT_OID, parameterErrorMap);
					}
						
				} else  {
					LOGGER_UNDERLYING_THREAD.debug("The underlying " + underlyingIdType + ": " + underlyingId + " does not exists in RDR. Inserting this underlying " + underlyingIdType + " in RDR DDBB");
					
					FT_T_ISSU issu = new FT_T_ISSU(entityManager);
					DataFields dataFieldsIssu = new DataFields();
					AuditFields auditFieldsIssu = new AuditFields();
					List<FT_T_ISID> isidSet = new ArrayList<FT_T_ISID>();
					
					String rdrId = UtilsMethods.createRDRId(entityManager);
					
					if (RIC.equals(underlyingIdType)) {
						TYPE_OF_IDENTIFIERS = new ArrayList<>( Arrays. asList("RIC", "RDR_ID"));
					} else {
						TYPE_OF_IDENTIFIERS = new ArrayList<>( Arrays. asList("ISIN", "RDR_ID"));
					}
					
					for (String identifier : TYPE_OF_IDENTIFIERS) {
						FT_T_ISID isid = new FT_T_ISID(entityManager);
						AuditFields auditFieldsIsid = new AuditFields();
						DataFields dataFieldsIsid = new DataFields();
						
						dataFieldsIsid.setDataSrcId(REFINITIV);
						dataFieldsIsid.setDataStatTyp(ACTIVE);
						
						auditFieldsIsid.setLastChgTms(new Date());
						auditFieldsIsid.setStartTms(new Date());
						auditFieldsIsid.setLastChgUsrId(LAST_CHANGE_USER);
						
						isid.setGlobalUniqInd(GLOBAL_UNIQ_IND);
						
						if (identifier.equals(RDR_ID)) {
							isid.setIdCtxtTyp(identifier);
							isid.setIssId(rdrId);
						} else  {
							isid.setIdCtxtTyp(identifier);
							isid.setIssId(underlyingId);
						}
						
						isid.setInstrId(issu.getInstrId());
						
						isid.setAuditFields(auditFieldsIsid);
						isid.setDataFields(dataFieldsIsid);
						
						isidSet.add(isid);
					}
					
					dataFieldsIssu.setDataSrcId(REFINITIV);
					dataFieldsIssu.setDataStatTyp(ACTIVE);
					
					auditFieldsIssu.setLastChgTms(new Date());
					auditFieldsIssu.setStartTms(new Date());
					auditFieldsIssu.setLastChgUsrId(LAST_CHANGE_USER);
					
					issu.setPrefIdCtxtTyp(underlyingIdType);
					issu.setPrefIssDesc(underlyingId);
					issu.setPrefIssId(underlyingId);
					issu.setPrefIssNme(underlyingId);
					issu.setIssAlphSrchTxt(rdrId);
					issu.setIssActvyStatTyp(ACTIVE);
					issu.setIssTyp(underlyingTypology);
					issu.setAuditFields(auditFieldsIssu);
					issu.setDataFields(dataFieldsIssu);
					issu.setIsidList(isidSet);
					
					entityManager.persist(issu);
					
					StaticData.addRelationUnderlyingInstrIdMap(underlyingId, issu.getInstrId());
					
				}
				
			}
			
		}

		/**
		 * 
		 * @param underlyingCurrencyCode
		 * @param underlyingRIC
		 */
		private static void loadUnderlyingMap(String underlyingCurrencyCode, String underlyingRIC, EntityManager entityManager) {
			
			if (underlyingCurrencyCode != null && !underlyingCurrencyCode.isEmpty()) {
				StaticData.addRelationUnderlyingCurrencyCodeMap(underlyingRIC, underlyingCurrencyCode);
			} else if (ExceptionService.vreqOid == null) {
				HashMap<String, String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("underlyingric", underlyingRIC);
				ExceptionService.createException(entityManager, ExceptionsLibrary.MARKET_CURRENCY_CODE_NOT_INFORMED, parameterErrorMap);
			}
						
			
		}
		
		private String calculateMktOid (List<FT_T_ISID> underlyingRicList, String RIC) {
			
			for (FT_T_ISID isid : underlyingRicList) {
				
				if (RIC.equals(isid.getIssId())) {
					return isid.getMktOid();
				}
			}
			
			return null;
		}
		
	}
	
}

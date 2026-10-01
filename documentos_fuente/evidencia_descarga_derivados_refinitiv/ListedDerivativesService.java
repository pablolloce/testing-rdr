package com.bbva.kytl.refinitivderivativesloader.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import com.bbva.kytl.refinitivderivativesloader.LoaderProcess;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISID;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISSU;
import com.bbva.kytl.refinitivderivativesloader.processors.DerivativesProcessor;
import com.bbva.kytl.refinitivderivativesloader.utils.ExceptionsLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.PositionsTemplate;
import com.bbva.kytl.refinitivderivativesloader.utils.QueryLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.StaticData;
import com.bbva.kytl.refinitivderivativesloader.utils.UtilsMethods;


public class ListedDerivativesService {

	private static Logger LOGGER_LISTED_DERIVATIVES = Logger.getLogger(ListedDerivativesService.class);
	
	private static final int maxNumberOfThreads = 20;
		
	private static final String OPTIONS = "OPTIONS";
		
	public static boolean loadListedDerivatives(File listedDerivativesFile) {
		
		StaticData.loadInitialStaticData();
		
		final ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxNumberOfThreads);
		
		try {
			LOGGER_LISTED_DERIVATIVES.debug("Reading file");
			BufferedReader csvReader = new BufferedReader(new FileReader(listedDerivativesFile));
			
			ArrayList<String> listLines = new ArrayList<String>();
			
			boolean last = false;
			String line;
			String nextLine =csvReader.readLine();
			
			while (!last) {
				
				line = nextLine;
				last =(nextLine = csvReader.readLine()) == null;
				
				listLines.add(line);
				
				if (listLines.size() == 50 || last) {
					poolExecutor.execute(new ListedDerivativesThread((List<String>) listLines.clone()));
					listLines.clear();
				}
				
			}
			csvReader.close();
			
			poolExecutor.shutdown();
			try {
				poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				LOGGER_LISTED_DERIVATIVES.error("Process out of time: ", e);
				return false;
			}
		} catch (IOException e) {
			LOGGER_LISTED_DERIVATIVES.error("Error reading file: ", e);
		}		
		
		return true;
		
	}
	
	
	private static class ListedDerivativesThread implements Runnable {

		private static Logger LOGGER_LISTED_DERIVATIVES_THREAD = Logger.getLogger(ListedDerivativesService.class);
		
		private List<String> derivativesList;
		
		private EntityManager entityManager;
		
		private static final String UNDERLYING_INACTIVE_CHARACETER = "^";
		
		private static final String INACTIVE = "INACTIVE";
		
		private static final String ACTIVE = "ACTIVE";
		
		private static final String REFINITIV = "REFINITIV";
		
		private static final String LAST_CHANGE_USER = "BBVA:CUSTOMER:REFINITIV:DERIVADOS";

		private static final String EXCHANGE_CODE_REU = "REU";
		
		private static final String ACTION_DELETE = "D";
		
		public ListedDerivativesThread(List<String> derivativesList) {
			this.derivativesList = derivativesList;
		}
		
		@Override
		public void run() {
			
			entityManager = LoaderProcess.entityManagerFactory.createEntityManager();
			TypedQuery<FT_T_ISSU> issuQuery;
			issuQuery = entityManager.createQuery(QueryLibrary.GET_INSTRUMENT_BY_IDENTIFIER, FT_T_ISSU.class); 
			List<FT_T_ISSU> issuResponseList;
			boolean searchByRIC = false;
			for (String line : derivativesList) {
				LOGGER_LISTED_DERIVATIVES_THREAD.debug("Start processing issue: " +  line);
				String[] splittedData = line.split("\\|",-1);
				
				if ((!splittedData[PositionsTemplate.UNDERLYING_RIC].isEmpty() && 
						splittedData[PositionsTemplate.UNDERLYING_RIC].contains(UNDERLYING_INACTIVE_CHARACETER)) ||
						INACTIVE.equals(StaticData.getStatusTranslationDataMap().get(splittedData[PositionsTemplate.ASSET_STATUS])) ||
						ACTION_DELETE.equals(splittedData[PositionsTemplate.ACTION]) ) {
					
					if (LoaderProcess.onlineExecution) {
						issuQuery.setParameter("idType", "RIC");
						issuQuery.setParameter("idValue", splittedData[PositionsTemplate.RIC]);
					} else {
						issuQuery.setParameter("idType", "QUOTE_PERM_ID");
						issuQuery.setParameter("idValue", splittedData[PositionsTemplate.QUOTE_PERM_ID]);
					}
					
					List<FT_T_ISSU> issuList = issuQuery.getResultList();
					if (issuList != null && issuList.size() == 1) {
						FT_T_ISSU issu = issuList.get(0);
						
						issu.setIssActvyStatTyp(UtilsMethods.transalateRefinitivStatus(issu.getIssActvyStatTyp()));
						issu.getDataFields().setDataStatTyp(INACTIVE);
						issu.getAuditFields().setLastChgTms(new Date());
						issu.getDataFields().setDataSrcId(REFINITIV);
						issu.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						for (FT_T_ISID isid : issu.getIsidList()) {
							isid.getDataFields().setDataStatTyp(INACTIVE);
							isid.getDataFields().setDataSrcId(REFINITIV);
							isid.getAuditFields().setLastChgTms(new Date());
							isid.getAuditFields().setLastChgUsrId(LAST_CHANGE_USER);
						}
						entityManager.persist(issu);
						
					}
					
				} else if (checkCurrency(splittedData) && 
						checkOptionsExerciseStyle(splittedData) &&
						checkMethodOfDelivery(splittedData) && 
						checkIfUnderlyingIsDuplicated(splittedData) &&
						checkIfExistsIssueTypTranslation(splittedData) && 
						checkMarketOid(splittedData)  
						) {
										
						if (LoaderProcess.onlineExecution) {
							LOGGER_LISTED_DERIVATIVES_THREAD.debug("Issue with Ric" + splittedData[PositionsTemplate.RIC] + "has pass all filters");
							issuQuery.setParameter("idType", "RIC");
							issuQuery.setParameter("idValue", splittedData[PositionsTemplate.RIC]);
							issuResponseList = issuQuery.getResultList();
							searchByRIC = true;
						} else {
							LOGGER_LISTED_DERIVATIVES_THREAD.debug("Issue with Quote Perm Id " + splittedData[PositionsTemplate.QUOTE_PERM_ID] + "has pass all filters");
							issuQuery.setParameter("idType", "QUOTE_PERM_ID");
							issuQuery.setParameter("idValue", splittedData[PositionsTemplate.QUOTE_PERM_ID]);
							issuResponseList = issuQuery.getResultList();
							
							if (issuResponseList == null || issuResponseList.isEmpty()) {
								TypedQuery<FT_T_ISSU> issuFakeQuery = entityManager.createQuery(QueryLibrary.GET_FAKE_INSTRUMENT_BY_RIC, FT_T_ISSU.class);
								issuFakeQuery.setParameter("idValue", splittedData[PositionsTemplate.RIC]);
								issuResponseList = issuFakeQuery.getResultList();
								searchByRIC = true;
							}
						}
						
						treatResponse(issuResponseList, splittedData, entityManager, searchByRIC);						
				}		
			}
			
			try {
				entityManager.getTransaction().begin();
				entityManager.getTransaction().commit();
			} catch (PersistenceException e) {
				LOGGER_LISTED_DERIVATIVES_THREAD.error("Error commiting entities" , e);
			} finally {
				entityManager.close();
			}
			
			
		}	
		
		
		private void treatResponse(List<FT_T_ISSU> issuResponseList, String[] data, EntityManager entityManager, boolean searchByRic) {
			
			
			if (issuResponseList != null && issuResponseList.size() > 1 ) {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				if (searchByRic) {
					parameterErrorMap.put("idType", "RIC");
					parameterErrorMap.put("idValue", data[PositionsTemplate.RIC]);
				} else {
					parameterErrorMap.put("idType", "QUOTE_PERM_ID");
					parameterErrorMap.put("idValue", data[PositionsTemplate.QUOTE_PERM_ID]);
				}
				
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_IS_DUPLICATED_ONLINE_VREQ, parameterErrorMap);
				} else {
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_IS_DUPLICATED, parameterErrorMap);
				}
				
			} else if (issuResponseList != null && issuResponseList.size() == 1 && checkIfMarketIsNotDuplicated(data) && checkIfDerivativeHasMoreThanOneUnderying(data)) {
				LOGGER_LISTED_DERIVATIVES_THREAD.debug("Issue with Quote Perm Id " + data[PositionsTemplate.QUOTE_PERM_ID] + " already exists in RDR");
				DerivativesProcessor.updateDerivativeData(issuResponseList.get(0), data, entityManager);
			} else if (issuResponseList.size() == 0 && ACTIVE.equals(StaticData.getStatusTranslationDataMap().get(data[PositionsTemplate.ASSET_STATUS]))){
				LOGGER_LISTED_DERIVATIVES_THREAD.debug("Issue with Quote Perm Id " + data[PositionsTemplate.QUOTE_PERM_ID] + " does not exists in RDR");
				DerivativesProcessor.insertDerivativeData(data, entityManager);
			}
			
		}
		
		/**
		 * 
		 * @param data
		 * @return
		 */
		private boolean checkIfMarketIsNotDuplicated(String[] data) {
			
			Query marketDuplicatedQuery = entityManager.createNativeQuery(QueryLibrary.CHECK_IF_MKT_IS_DUPLICATED_IN_RDR);
			marketDuplicatedQuery.setParameter("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
			
			if (((BigDecimal) marketDuplicatedQuery.getSingleResult()).doubleValue() > 1) {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_HAS_MORE_THAN_ONE_MARKET_ONLINE_VREQ, parameterErrorMap);
				} else {
					parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_HAS_MORE_THAN_ONE_MARKET, parameterErrorMap);
				}
				
				return false;
			} else {
				return true;
			}
			
		}
		
		/**
		 * 
		 * @param data
		 * @return
		 */
		private boolean checkIfDerivativeHasMoreThanOneUnderying(String[] data) {
			String underlyingRic =  data[PositionsTemplate.UNDERLYING_RIC];
			if (!underlyingRic.isEmpty()) {
				Query rissDuplicatedQuery = entityManager.createNativeQuery(QueryLibrary.CHECK_IF_RISS_IS_DUPLICATED_IN_RDR);
				rissDuplicatedQuery.setParameter("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
				if (((BigDecimal) rissDuplicatedQuery.getSingleResult()).doubleValue() > 1) {
					HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
					if (ExceptionService.vreqOid != null) {
						ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_HAS_MORE_THAN_ONE_UNDERLYING_ONLINE_VREQ, parameterErrorMap);
					} else {
						parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
						ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_HAS_MORE_THAN_ONE_UNDERLYING, parameterErrorMap);
					}
					
					
					return false;
				} else {
					return true;
				}
				
			}
			return true;
			
		}
		
		/**
		 * 
		 * @param data
		 * @return
		 */
		private boolean checkIfExistsIssueTypTranslation(String[] data) {
			
			if (StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]) != null) {
				return true;
			} else {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("refinitivClassificationScheme", data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]);
				
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.ISSUE_TYPE_DOES_NOT_EXISTS_IN_RDR_ONLINE_VREQ, parameterErrorMap);
				} else {
					parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
					ExceptionService.createException(entityManager, ExceptionsLibrary.ISSUE_TYPE_DOES_NOT_EXISTS_IN_RDR, parameterErrorMap);
				}
				
				return false;
			}
			
		}
		
		/**
		 * 
		 * @param data
		 * @return
		 */
		private boolean checkOptionsExerciseStyle(String[] data) {
			
			if (StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]) != null && 
					OPTIONS.equals(StaticData.getIssueTypeMap().get(data[PositionsTemplate.REFINITIV_CLASSIFICATION_SCHEME]).getIssTyp())) {
				
				if (StaticData.getOptionsExerciseStyles().contains(data[PositionsTemplate.EXERCISE_STYLE])) {
					return true;
				} else {
					HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
					parameterErrorMap.put("exerciseStyle", data[PositionsTemplate.EXERCISE_STYLE]);
					if (ExceptionService.vreqOid != null) {
						ExceptionService.createException(entityManager, ExceptionsLibrary.OPTION_EXERCISE_STYLE_DOES_NOT_EXIST_IN_RDR_ONLINE_VREQ, parameterErrorMap);
					} else {
						parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
						ExceptionService.createException(entityManager, ExceptionsLibrary.OPTION_EXERCISE_STYLE_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
					}
					
					return false;
				}
				
			} else {
				return true;
			}
		}
		
		/**
		 * Method to check if the currency exists in RDR. If the currency does not exists create an exception in DDBB
		 * @param data derivatives data
		 * @return true if currency exists in RDR and false if not
		 */
		private boolean checkCurrency(String[] data) {
			
			String currency = data[PositionsTemplate.CURRENCY];
			
			if (StaticData.getCurrencyList().contains(currency)) {
				return true;
			} else {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("currencyCode", currency);
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.CURRENCY_DOES_NOT_EXIST_IN_RDR_ONLINE_VREQ, parameterErrorMap);
				} else {
					parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
					ExceptionService.createException(entityManager, ExceptionsLibrary.CURRENCY_DOES_NOT_EXIST_IN_RDR, parameterErrorMap);
				}
				
				return false;
			}	
		}
		
		/**
		 * Method to check if the method of delivery exists in RDR. If it does not exists create an exception in DDBB
		 * @param data derivatives data
		 * @return true if method of delivery exists in RDR and false if not
		 */
		private boolean checkMethodOfDelivery(String[] data) {
			String methodOfDelivery = data[PositionsTemplate.METHOD_OF_DELIVERY];
			String methodOfDeliveryTranslation = StaticData.getMethodOfDeliveryTranslationMap().get(methodOfDelivery);
			if (methodOfDeliveryTranslation != null && !methodOfDeliveryTranslation.isEmpty()) {
				return true;
			} else {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("methodOfDelivery", methodOfDelivery);
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.METHOD_OF_DELIVERY_EXCEPTION_ONLINE_VREQ, parameterErrorMap);
				} else {
					parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
					ExceptionService.createException(entityManager, ExceptionsLibrary.METHOD_OF_DELIVERY_EXCEPTION, parameterErrorMap);
				}
				
				return false;
			}
			
		}
		
		/**
		 * Method to check if the Derivative´s underlyings is duplicated in RDR
		 * @param data derivatives data
		 * @return true if it is not duplicated and false if it is
		 */
		private boolean checkIfUnderlyingIsDuplicated(String[] data) {
			
			String underlyingId, underlyingIdType;
			
			if (data[PositionsTemplate.UNDERLYING_RIC] != null && !data[PositionsTemplate.UNDERLYING_RIC].isEmpty()) {
				underlyingId = data[PositionsTemplate.UNDERLYING_RIC];
				underlyingIdType = "RIC";
			} else if (data[PositionsTemplate.UNDERLYING_ISIN] != null && !data[PositionsTemplate.UNDERLYING_ISIN].isEmpty()){
				underlyingId = data[PositionsTemplate.UNDERLYING_ISIN];
				underlyingIdType = "ISIN";
			} else if (data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN] != null && !data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN].isEmpty()){
				underlyingId = data[PositionsTemplate.UNDERLYING_CHEAPEST_ISIN];
				underlyingIdType = "ISIN";
			} else{
				underlyingId = data[PositionsTemplate.UNDERLYING_ISIN_ESMA];
				underlyingIdType = "ISIN";
			}
			
			
			if (StaticData.getDuplicatedUnderlyings().contains(underlyingId)) {
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("underlyingId", underlyingId);
				parameterErrorMap.put("underIdTpye", underlyingIdType);
				if (ExceptionService.vreqOid != null) {
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_UNDERLYING_DUPLICATED_ONLINE_VREQ, parameterErrorMap);
				} else {
					parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
					ExceptionService.createException(entityManager, ExceptionsLibrary.DERIVATIVE_UNDERLYING_DUPLICATED, parameterErrorMap);
				}
				
				return false;
			} else {
				return true;
			}
			
		}
		
		/**
		 * 
		 * @param data
		 * @return
		 */
		private boolean checkMarketOid(String[] data) {
			
			if (data[PositionsTemplate.MIC] != null && !data[PositionsTemplate.MIC].isEmpty()) {
				
				if (StaticData.getMarketGuntMapMic().get(data[PositionsTemplate.MIC]) != null) {
					return true;
				} else {
					HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
					parameterErrorMap.put("mic", data[PositionsTemplate.MIC]);
					if (ExceptionService.vreqOid != null) {
						ExceptionService.createException(entityManager, ExceptionsLibrary.MIC_IDENTIFIER_DOES_NOT_EXISTS_IN_RDR_ONLINE_VREQ, parameterErrorMap);
					} else {
						parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
						ExceptionService.createException(entityManager, ExceptionsLibrary.MIC_IDENTIFIER_DOES_NOT_EXISTS_IN_RDR, parameterErrorMap);
					}
					
					return false;
				}
				
			} else if (data[PositionsTemplate.EXCHANGE_CODE] != null && !data[PositionsTemplate.EXCHANGE_CODE].isEmpty() && !EXCHANGE_CODE_REU.equals(data[PositionsTemplate.EXCHANGE_CODE])) {
				
				if (StaticData.getMarketGuntMapExchangeCode().get(data[PositionsTemplate.EXCHANGE_CODE]) != null) {
					return true;
				} else {
					HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
					parameterErrorMap.put("exchangeCode", data[PositionsTemplate.EXCHANGE_CODE]);
					if (ExceptionService.vreqOid != null) {
						ExceptionService.createException(entityManager, ExceptionsLibrary.EXCHANGE_CODE_DOES_NOT_EXISTS_IN_RDR_ONLINE_VREQ, parameterErrorMap);
					} else {
						parameterErrorMap.put("quotePermId", data[PositionsTemplate.QUOTE_PERM_ID]);
						ExceptionService.createException(entityManager, ExceptionsLibrary.EXCHANGE_CODE_DOES_NOT_EXISTS_IN_RDR, parameterErrorMap);
					}

					return false;
				}
				
			} else {
				return true;
			}
			
		}
		
		
		
	}
	
	
	
}

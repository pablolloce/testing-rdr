package com.bbva.kytl.refinitivderivativesloader.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
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
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_FINS;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_ISSR;
import com.bbva.kytl.refinitivderivativesloader.utils.ExceptionsLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.QueryLibrary;
import com.bbva.kytl.refinitivderivativesloader.utils.StaticData;

public class IssuersService {

	private static Logger LOGGER_ISSUER_MAIN = Logger.getLogger(IssuersService.class);
	
	private static final int maxNumberOfThreads = 10; 
	
	public static boolean loadIssuers(File issuerFile) {
	
		final ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxNumberOfThreads);
		
		try {
			LOGGER_ISSUER_MAIN.debug("Reading file");
			BufferedReader csvReader = new BufferedReader(new FileReader(issuerFile));
			
			ArrayList<String> listLines = new ArrayList<String>();
			
			boolean last = false;
			String line;
			String nextLine =csvReader.readLine();
			
			while (!last) {
				line = nextLine;
				last =(nextLine = csvReader.readLine()) == null;
				listLines.add(line);
				
				if (listLines.size() == 500 || last) {
					poolExecutor.execute(new IssuerThread((List<String>) listLines.clone()));
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
			LOGGER_ISSUER_MAIN.error("Issuer file could no be found:_" , e);
			return false;
			
		} catch (Exception e) {
			LOGGER_ISSUER_MAIN.error("Error in Issuers Load Process", e);
		}
		
		poolExecutor.shutdown();
		try {
			poolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOGGER_ISSUER_MAIN.error("Process out of time");
			e.printStackTrace();
			return false;
		}
		
		return true;
		
	}
	
	
	private static class IssuerThread implements Runnable {

		private static Logger LOGGER_ISSUER_THREAD = Logger.getLogger(IssuerThread.class);

		private List<String> orgIdsList;
		
		private EntityManager entityManager;
		
		private static final String LAST_CHANGE_USER = "BBVA:CUSTOMER:REFINITIV:DERIVADOS";
		
		private static final String REFINITIV = "REFINITIV";
		
		private static final String ACTIVE = "ACTIVE";
		
		private static final String ISSUER = "ISSUER";
		
		private IssuerThread(List<String> lines) {
			this.orgIdsList = lines;
		}
		
		public void run() {
			
			entityManager = LoaderProcess.entityManagerFactory.createEntityManager();
			
			TypedQuery<FT_T_FINS> finsQuery = null;
			
			for (String orgId : orgIdsList) {
				
				if (orgId != null && !orgId.isEmpty()) {
					finsQuery = entityManager.createQuery(QueryLibrary.GET_INST_MNEM_QUERY, FT_T_FINS.class);
					
					finsQuery.setParameter("finrId", orgId);
					
					treatResponse(finsQuery.getResultList(), orgId);
				}
				
			}
			
			entityManager.getTransaction().begin();
			entityManager.getTransaction().commit();
			entityManager.close();
			
		}
		
		/**
		 * Method to treat FT_T_FINS list  response
		 * @param finsResponseList
		 */
		private void treatResponse(List<FT_T_FINS> finsResponseList, String orgId) {
			
			boolean isCptyCorrectInRdr = true;
			
			if (finsResponseList.isEmpty() && ExceptionService.vreqOid == null) {
				LOGGER_ISSUER_THREAD.debug("The issuer with orgId: " + orgId + " does not existe in RDR or is not correctly informed");
				HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
				parameterErrorMap.put("orgid", orgId);
				ExceptionService.createException(entityManager, ExceptionsLibrary.ISSUER_NOT_EXIST_OR_NOT_CORRECT_INFORMED, parameterErrorMap);
				
			} else {
				
				FT_T_FINS finsEntity =finsResponseList.get(0); 
				
				int hashCode = finsEntity.hashCode();
				
				for (FT_T_FINS fins : finsResponseList) {
					
					if(hashCode != fins.hashCode() && ExceptionService.vreqOid == null) {
						isCptyCorrectInRdr = false;
						HashMap<String,String> parameterErrorMap = new HashMap<String, String>();
						parameterErrorMap.put("orgid", orgId);
						ExceptionService.createException(entityManager, ExceptionsLibrary.ISSUER_DUPLICATED_EXCEPTION, parameterErrorMap);
						break;
					}
					
				}
				
				if (isCptyCorrectInRdr) {
					
					TypedQuery<FT_T_ISSR> issrQuery = entityManager.createQuery(QueryLibrary.GET_ISSR_BY_INST_MNEM_QUERY, FT_T_ISSR.class);
					
					issrQuery.setParameter("instMnem", finsEntity.getInstMnem());
					
					List<FT_T_ISSR> issrList = issrQuery.getResultList();
					
					if (issrList == null || issrList.isEmpty()) {
						
						LOGGER_ISSUER_THREAD.debug("Creating relation in FT_T_ISSR entity between the issuer with orgId " + orgId + " and the corresponding issu");
						
						FT_T_ISSR issr = new FT_T_ISSR(entityManager);
						AuditFields auditFields = new AuditFields();
						DataFields dataFields = new DataFields();
						
						auditFields.setStartTms(new Date());
						auditFields.setLastChgTms(new Date());
						auditFields.setLastChgUsrId(LAST_CHANGE_USER);
						
						dataFields.setDataSrcId(REFINITIV);
						dataFields.setDataStatTyp(ACTIVE);
						
						issr.setAuditFields(auditFields);
						issr.setDataFields(dataFields);
						
						issr.setIssrNme(finsEntity.getInstNme());
						issr.setInstMnem(finsEntity.getInstMnem());
						issr.setFinsInstMnem(finsEntity.getInstMnem());
						issr.setFinsrlTyp(ISSUER);
						entityManager.persist(issr);
						
						
						StaticData.addRelationToIssuerDerivativeMap(orgId, issr.getInstrIssrId());
						
					} else {
						StaticData.addRelationToIssuerDerivativeMap(orgId, issrList.get(0).getInstrIssrId());
					}
					
				}
			}
			
		}
		
	}
	
}

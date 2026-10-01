package com.bbva.kytl.refinitivderivativesloader;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.bbva.kytl.refinitivderivativesloader.connection.OracleConnection;
import com.bbva.kytl.refinitivderivativesloader.entities.FT_T_VREQ;
import com.bbva.kytl.refinitivderivativesloader.services.ExceptionService;
import com.bbva.kytl.refinitivderivativesloader.services.IssuersService;
import com.bbva.kytl.refinitivderivativesloader.services.ListedDerivativesService;
import com.bbva.kytl.refinitivderivativesloader.services.UnderlyingService;
import com.bbva.kytl.refinitivderivativesloader.utils.QueryLibrary;



/**
 * Load process of Refinitiv Derivative Listed
 * @author Nfq
 *
 */
public class LoaderProcess 
{
	
	public static EntityManagerFactory entityManagerFactory;
	
	public static Logger LOGGER;
	
	private static File issuerFile;
	
	private static File underlyingFile;
	
	private static File derivativesFile;
	
	public static final String ONLINE = "ONLINE";
	
	public static boolean onlineExecution = false;
	
	private static boolean loadProcessStatus = true;
	
	/** Constant used to identify Issuers file*/
	private static final String ISSUERS = "Emisores";
	
	/** Constant used to identify Underlyings file*/
	private static final String UNDERLYINGS = "Subyacentes";
	
	/** Constant used to identify Listed Derivatives file*/
	private static final String DERIVATIVES = "Derivados_Enriquecido";
	
	private static final String PROCESSED = "PROCESSED";
	
	private static final String FAILED = "FAILED";
	
    public static void main( String[] args ) {
    	
    	System.setProperty("org.jboss.logging.provider", "log4j");
    	
    	LOGGER = Logger.getLogger(LoaderProcess.class);
    	
    	PropertyConfigurator.configure(args[0]);
    	
    	LOGGER.info("Initizated refinitiv data filter process");
    	
     	if (ONLINE.equals(args[1])) {
    		onlineExecution = true;
    	}
    	
     	if (args.length == 5) {
     		ExceptionService.vreqOid = args[4];
     	}
     	
    	File folder = new File(args[2]);
    	File[] filesToProcess = folder.listFiles((f,name)-> name.contains(args[3]));
    	
    	if (filesToProcess.length == 3) {
    		setFiles(filesToProcess);
    	} else {
    		LOGGER.error("Execution needs 3 files. " + filesToProcess.length + " was found");
    	}

    	ApplicationContext appContext = new AnnotationConfigApplicationContext(OracleConnection.class);
    	
    	entityManagerFactory = (EntityManagerFactory) appContext.getBean("entityManagerFactory");
    	    	
    	/** Process Issuer file*/
    	if (issuerFile != null) {
    		loadProcessStatus  = IssuersService.loadIssuers(issuerFile);
    	}
    	
    	/** Process Underlyings file*/
    	if (loadProcessStatus && underlyingFile != null) {
    		loadProcessStatus = UnderlyingService.loadUnderlyings(underlyingFile);
    	}

    	/** Process listed derivatives file*/
    	if (loadProcessStatus && derivativesFile != null) {
    		loadProcessStatus = ListedDerivativesService.loadListedDerivatives(derivativesFile);
    	}

    	if (ExceptionService.vreqOid != null) {
    		setVreqStatus();
    	}
    	
    	
    }
    
    /**
     * Method to identify each file from the directory
     * @param files files in the directory
     */
    private static void setFiles (File[] files) {
    	
    	for (int i = 0 ; i < files.length ; i++) {
    		
    		if (files[i].getName().contains(ISSUERS)) {
    			issuerFile = files[i];
    			continue;
	    	}
    		
    		if (files[i].getName().contains(UNDERLYINGS)) {
    			underlyingFile = files[i];
    			continue;
	    	}
    		
    		if (files[i].getName().contains(DERIVATIVES)) {
    			derivativesFile = files[i];
	    	}
    			
    	}
    	
    	
    }
    
    /**
     * 
     */
    private static void setVreqStatus() {
    	EntityManager entityManager = LoaderProcess.entityManagerFactory.createEntityManager();
    	TypedQuery<FT_T_VREQ> vreqQuery = entityManager.createQuery(QueryLibrary.QUERY_GET_VREQ_ONLINE, FT_T_VREQ.class);
    	vreqQuery.setParameter("vreqOid", ExceptionService.vreqOid);
    	List<FT_T_VREQ> vreqList = vreqQuery.getResultList();
    	
    	if (vreqList.size() != 1) {
    		LOGGER.error("Error while trying to get FT_T_VREQ register whit VREQ_OID = " + ExceptionService.vreqOid);
    	} else {
    		FT_T_VREQ vreq = vreqList.get(0);
    		
    		if (!FAILED.equals(vreq.getVndRqstStatTyp())) {
    			vreq.setVndRqstStatTyp(PROCESSED);
        		vreq.setLastChgTms(new Date());
        		entityManager.persist(vreq);
        		entityManager.getTransaction().begin();
				entityManager.getTransaction().commit();
				
    		}
    		
    	}
    	entityManager.close();
    }
    
}

package com.bbva.kytl.refinitivderivativesloader.entities;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table (name= "FT_T_FINR", schema="KYTL_GC")
public class FT_T_FINR {

	@Column(name = "INST_MNEM")
	private String instMnem;
	
	@Column(name = "CROSS_REF_ID")
	private String crossRefId;
	
	@Column(name = "PREF_CURR_CDE")
	private String PrefCurrCde;
	
	@Column(name = "MAIL_ADDR_ID")
	private String mailAddrId;
	
	@Column(name = "ELEC_ADDR_ID")
	private String elecAddrId;
	
	@Column(name = "PREF_ISS_CTXT_TYP")
	private String pressIssCtxtTyp;
	
	@Column(name = "FINSRL_TYP")
	private String finsrlTyp;
	
	@Column(name = "CAL_ID")
	private String calId;
	
	@Column(name = "DAYS_TYP")
	private String daysTyp;
	
	@Column(name = "ACTS_PYNG_AGNT_IND")
	private String actsPyngAgntInd;
	
	@Column(name = "VAL_DAYS_OF_NUM")
	private String valDaysOfNum;
	
	@Column(name = "TRD_RPTG_MNEM")
	private String trdRptgMnem;
	
	@Column(name = "FINSRL_CONTCT_TXT")
	private String finsrlContctTxt;
	
	@Column(name = "FINSRL_DESC")
	private String finsrlDesc;
	
	@Column(name = "NOFIX_SETTLE_DESC")
	private String nofixSettleDesc;
	
	@Column(name = "FINSRL_NME")
	private String finsrlNme;
	
	@Column(name = "PREF_ID_CTXT_TYP")
	private String prefIdCtxtTyp;
	
	@Column(name = "START_BUS_DY_TME")
	@Temporal(TemporalType.TIMESTAMP)
	private Date startBusDyTme;
	
	@Column(name = "END_BUS_DY_TME")
	@Temporal(TemporalType.TIMESTAMP)
	private Date endBusDyTms;
	
	@Column(name = "SRO_JURIS_EFF_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date sroJurisEffDte;
	
	@Column(name = "CONTCT_OID")
	private String contctOid;
	
	@Column(name = "PREF_FINR_ID")
	private String prefFinrId;
	
	@Column(name = "RCPT_PAY_TYP")
	private String rcptPayTyp;
	
	@Column(name = "DLV_PAY_TYP")
	private String dlvPayTyp;
	
	@Column(name = "AUTH_UK_INTERMEDIARY_IND")
	private String authUkIntermediaryInd;
	
	@Column(name = "DFLT_CORR_BNK_IND")
	private String dfltCorrBnkInd;
	
	@Column(name = "FINSRL_SUB_TYP")
	private String finsrlSubTyp;
	
	@Column(name = "QI_CAPACITY_TYP")
	private String qiCapacityTyp;
	
	@Column(name = "CLAIM_IND")
	private String claimInd;
	
	@Column(name = "FINSRL_STAT_TYP")
	private String finsrlStatTyp;
	
	@Column(name = "FINSRL_STAT_TMS")
	@Temporal(TemporalType.TIMESTAMP)
	private Date finsrlStatTms;
	
	@Column(name = "CLIENT_SRVC_TYP")
	private String clientSrvcTyp;
	
	@Column(name = "WEB_PORTAL_IND")
	private String webPortalInd;
	
	@Column(name = "CLIENT_RST_IND")
	private String clientRstInd;
	
	@Column(name = "PRIN_REG_JURIS_ID")
	private String prinRegJurisId;
	
	@Column(name = "PREF_SETTLE_TYP")
	private String prefSettleTyp;
	
	@Column(name = "PAY_METH_TYP")
	private String payMethTyp;
	
	@Column(name = "MSG_FMT_MNEM")
	private String msgFmtMnem;
	
	@Column(name = "GLOBAL_DATA_PROV_IND")
	private String globalDataProvInd;
	
	@Id
	@Column(name = "FINR_OID")
	private String FINR_OID;
	
	@Embedded
	private DataFields dataFields;
	
	@Embedded
	private AuditFields auditFields;

	/**
	 * @return the instMnem
	 */
	public String getInstMnem() {
		return instMnem;
	}

	/**
	 * @param instMnem the instMnem to set
	 */
	public void setInstMnem(String instMnem) {
		this.instMnem = instMnem;
	}

	/**
	 * @return the crossRefId
	 */
	public String getCrossRefId() {
		return crossRefId;
	}

	/**
	 * @param crossRefId the crossRefId to set
	 */
	public void setCrossRefId(String crossRefId) {
		this.crossRefId = crossRefId;
	}

	/**
	 * @return the prefCurrCde
	 */
	public String getPrefCurrCde() {
		return PrefCurrCde;
	}

	/**
	 * @param prefCurrCde the prefCurrCde to set
	 */
	public void setPrefCurrCde(String prefCurrCde) {
		PrefCurrCde = prefCurrCde;
	}

	/**
	 * @return the mailAddrId
	 */
	public String getMailAddrId() {
		return mailAddrId;
	}

	/**
	 * @param mailAddrId the mailAddrId to set
	 */
	public void setMailAddrId(String mailAddrId) {
		this.mailAddrId = mailAddrId;
	}

	/**
	 * @return the elecAddrId
	 */
	public String getElecAddrId() {
		return elecAddrId;
	}

	/**
	 * @param elecAddrId the elecAddrId to set
	 */
	public void setElecAddrId(String elecAddrId) {
		this.elecAddrId = elecAddrId;
	}

	/**
	 * @return the pressIssCtxtTyp
	 */
	public String getPressIssCtxtTyp() {
		return pressIssCtxtTyp;
	}

	/**
	 * @param pressIssCtxtTyp the pressIssCtxtTyp to set
	 */
	public void setPressIssCtxtTyp(String pressIssCtxtTyp) {
		this.pressIssCtxtTyp = pressIssCtxtTyp;
	}

	/**
	 * @return the finsrlTyp
	 */
	public String getFinsrlTyp() {
		return finsrlTyp;
	}

	/**
	 * @param finsrlTyp the finsrlTyp to set
	 */
	public void setFinsrlTyp(String finsrlTyp) {
		this.finsrlTyp = finsrlTyp;
	}

	/**
	 * @return the calId
	 */
	public String getCalId() {
		return calId;
	}

	/**
	 * @param calId the calId to set
	 */
	public void setCalId(String calId) {
		this.calId = calId;
	}

	/**
	 * @return the daysTyp
	 */
	public String getDaysTyp() {
		return daysTyp;
	}

	/**
	 * @param daysTyp the daysTyp to set
	 */
	public void setDaysTyp(String daysTyp) {
		this.daysTyp = daysTyp;
	}

	/**
	 * @return the actsPyngAgntInd
	 */
	public String getActsPyngAgntInd() {
		return actsPyngAgntInd;
	}

	/**
	 * @param actsPyngAgntInd the actsPyngAgntInd to set
	 */
	public void setActsPyngAgntInd(String actsPyngAgntInd) {
		this.actsPyngAgntInd = actsPyngAgntInd;
	}

	/**
	 * @return the valDaysOfNum
	 */
	public String getValDaysOfNum() {
		return valDaysOfNum;
	}

	/**
	 * @param valDaysOfNum the valDaysOfNum to set
	 */
	public void setValDaysOfNum(String valDaysOfNum) {
		this.valDaysOfNum = valDaysOfNum;
	}

	/**
	 * @return the trdRptgMnem
	 */
	public String getTrdRptgMnem() {
		return trdRptgMnem;
	}

	/**
	 * @param trdRptgMnem the trdRptgMnem to set
	 */
	public void setTrdRptgMnem(String trdRptgMnem) {
		this.trdRptgMnem = trdRptgMnem;
	}

	/**
	 * @return the finsrlContctTxt
	 */
	public String getFinsrlContctTxt() {
		return finsrlContctTxt;
	}

	/**
	 * @param finsrlContctTxt the finsrlContctTxt to set
	 */
	public void setFinsrlContctTxt(String finsrlContctTxt) {
		this.finsrlContctTxt = finsrlContctTxt;
	}

	/**
	 * @return the finsrlDesc
	 */
	public String getFinsrlDesc() {
		return finsrlDesc;
	}

	/**
	 * @param finsrlDesc the finsrlDesc to set
	 */
	public void setFinsrlDesc(String finsrlDesc) {
		this.finsrlDesc = finsrlDesc;
	}

	/**
	 * @return the nofixSettleDesc
	 */
	public String getNofixSettleDesc() {
		return nofixSettleDesc;
	}

	/**
	 * @param nofixSettleDesc the nofixSettleDesc to set
	 */
	public void setNofixSettleDesc(String nofixSettleDesc) {
		this.nofixSettleDesc = nofixSettleDesc;
	}

	/**
	 * @return the finsrlNme
	 */
	public String getFinsrlNme() {
		return finsrlNme;
	}

	/**
	 * @param finsrlNme the finsrlNme to set
	 */
	public void setFinsrlNme(String finsrlNme) {
		this.finsrlNme = finsrlNme;
	}

	/**
	 * @return the prefIdCtxtTyp
	 */
	public String getPrefIdCtxtTyp() {
		return prefIdCtxtTyp;
	}

	/**
	 * @param prefIdCtxtTyp the prefIdCtxtTyp to set
	 */
	public void setPrefIdCtxtTyp(String prefIdCtxtTyp) {
		this.prefIdCtxtTyp = prefIdCtxtTyp;
	}

	/**
	 * @return the startBusDyTme
	 */
	public Date getStartBusDyTme() {
		return startBusDyTme;
	}

	/**
	 * @param startBusDyTme the startBusDyTme to set
	 */
	public void setStartBusDyTme(Date startBusDyTme) {
		this.startBusDyTme = startBusDyTme;
	}

	/**
	 * @return the endBusDyTms
	 */
	public Date getEndBusDyTms() {
		return endBusDyTms;
	}

	/**
	 * @param endBusDyTms the endBusDyTms to set
	 */
	public void setEndBusDyTms(Date endBusDyTms) {
		this.endBusDyTms = endBusDyTms;
	}

	/**
	 * @return the sroJurisEffDte
	 */
	public Date getSroJurisEffDte() {
		return sroJurisEffDte;
	}

	/**
	 * @param sroJurisEffDte the sroJurisEffDte to set
	 */
	public void setSroJurisEffDte(Date sroJurisEffDte) {
		this.sroJurisEffDte = sroJurisEffDte;
	}

	/**
	 * @return the contctOid
	 */
	public String getContctOid() {
		return contctOid;
	}

	/**
	 * @param contctOid the contctOid to set
	 */
	public void setContctOid(String contctOid) {
		this.contctOid = contctOid;
	}

	/**
	 * @return the prefFinrId
	 */
	public String getPrefFinrId() {
		return prefFinrId;
	}

	/**
	 * @param prefFinrId the prefFinrId to set
	 */
	public void setPrefFinrId(String prefFinrId) {
		this.prefFinrId = prefFinrId;
	}

	/**
	 * @return the rcptPayTyp
	 */
	public String getRcptPayTyp() {
		return rcptPayTyp;
	}

	/**
	 * @param rcptPayTyp the rcptPayTyp to set
	 */
	public void setRcptPayTyp(String rcptPayTyp) {
		this.rcptPayTyp = rcptPayTyp;
	}

	/**
	 * @return the dlvPayTyp
	 */
	public String getDlvPayTyp() {
		return dlvPayTyp;
	}

	/**
	 * @param dlvPayTyp the dlvPayTyp to set
	 */
	public void setDlvPayTyp(String dlvPayTyp) {
		this.dlvPayTyp = dlvPayTyp;
	}

	/**
	 * @return the authUkIntermediaryInd
	 */
	public String getAuthUkIntermediaryInd() {
		return authUkIntermediaryInd;
	}

	/**
	 * @param authUkIntermediaryInd the authUkIntermediaryInd to set
	 */
	public void setAuthUkIntermediaryInd(String authUkIntermediaryInd) {
		this.authUkIntermediaryInd = authUkIntermediaryInd;
	}

	/**
	 * @return the dfltCorrBnkInd
	 */
	public String getDfltCorrBnkInd() {
		return dfltCorrBnkInd;
	}

	/**
	 * @param dfltCorrBnkInd the dfltCorrBnkInd to set
	 */
	public void setDfltCorrBnkInd(String dfltCorrBnkInd) {
		this.dfltCorrBnkInd = dfltCorrBnkInd;
	}

	/**
	 * @return the finsrlSubTyp
	 */
	public String getFinsrlSubTyp() {
		return finsrlSubTyp;
	}

	/**
	 * @param finsrlSubTyp the finsrlSubTyp to set
	 */
	public void setFinsrlSubTyp(String finsrlSubTyp) {
		this.finsrlSubTyp = finsrlSubTyp;
	}

	/**
	 * @return the qiCapacityTyp
	 */
	public String getQiCapacityTyp() {
		return qiCapacityTyp;
	}

	/**
	 * @param qiCapacityTyp the qiCapacityTyp to set
	 */
	public void setQiCapacityTyp(String qiCapacityTyp) {
		this.qiCapacityTyp = qiCapacityTyp;
	}

	/**
	 * @return the claimInd
	 */
	public String getClaimInd() {
		return claimInd;
	}

	/**
	 * @param claimInd the claimInd to set
	 */
	public void setClaimInd(String claimInd) {
		this.claimInd = claimInd;
	}

	/**
	 * @return the finsrlStatTyp
	 */
	public String getFinsrlStatTyp() {
		return finsrlStatTyp;
	}

	/**
	 * @param finsrlStatTyp the finsrlStatTyp to set
	 */
	public void setFinsrlStatTyp(String finsrlStatTyp) {
		this.finsrlStatTyp = finsrlStatTyp;
	}

	/**
	 * @return the finsrlStatTms
	 */
	public Date getFinsrlStatTms() {
		return finsrlStatTms;
	}

	/**
	 * @param finsrlStatTms the finsrlStatTms to set
	 */
	public void setFinsrlStatTms(Date finsrlStatTms) {
		this.finsrlStatTms = finsrlStatTms;
	}

	/**
	 * @return the clientSrvcTyp
	 */
	public String getClientSrvcTyp() {
		return clientSrvcTyp;
	}

	/**
	 * @param clientSrvcTyp the clientSrvcTyp to set
	 */
	public void setClientSrvcTyp(String clientSrvcTyp) {
		this.clientSrvcTyp = clientSrvcTyp;
	}

	/**
	 * @return the webPortalInd
	 */
	public String getWebPortalInd() {
		return webPortalInd;
	}

	/**
	 * @param webPortalInd the webPortalInd to set
	 */
	public void setWebPortalInd(String webPortalInd) {
		this.webPortalInd = webPortalInd;
	}

	/**
	 * @return the clientRstInd
	 */
	public String getClientRstInd() {
		return clientRstInd;
	}

	/**
	 * @param clientRstInd the clientRstInd to set
	 */
	public void setClientRstInd(String clientRstInd) {
		this.clientRstInd = clientRstInd;
	}

	/**
	 * @return the prinRegJurisId
	 */
	public String getPrinRegJurisId() {
		return prinRegJurisId;
	}

	/**
	 * @param prinRegJurisId the prinRegJurisId to set
	 */
	public void setPrinRegJurisId(String prinRegJurisId) {
		this.prinRegJurisId = prinRegJurisId;
	}

	/**
	 * @return the prefSettleTyp
	 */
	public String getPrefSettleTyp() {
		return prefSettleTyp;
	}

	/**
	 * @param prefSettleTyp the prefSettleTyp to set
	 */
	public void setPrefSettleTyp(String prefSettleTyp) {
		this.prefSettleTyp = prefSettleTyp;
	}

	/**
	 * @return the payMethTyp
	 */
	public String getPayMethTyp() {
		return payMethTyp;
	}

	/**
	 * @param payMethTyp the payMethTyp to set
	 */
	public void setPayMethTyp(String payMethTyp) {
		this.payMethTyp = payMethTyp;
	}

	/**
	 * @return the msgFmtMnem
	 */
	public String getMsgFmtMnem() {
		return msgFmtMnem;
	}

	/**
	 * @param msgFmtMnem the msgFmtMnem to set
	 */
	public void setMsgFmtMnem(String msgFmtMnem) {
		this.msgFmtMnem = msgFmtMnem;
	}

	/**
	 * @return the globalDataProvInd
	 */
	public String getGlobalDataProvInd() {
		return globalDataProvInd;
	}

	/**
	 * @param globalDataProvInd the globalDataProvInd to set
	 */
	public void setGlobalDataProvInd(String globalDataProvInd) {
		this.globalDataProvInd = globalDataProvInd;
	}

	/**
	 * @return the fINR_OID
	 */
	public String getFINR_OID() {
		return FINR_OID;
	}

	/**
	 * @param fINR_OID the fINR_OID to set
	 */
	public void setFINR_OID(String fINR_OID) {
		FINR_OID = fINR_OID;
	}

	/**
	 * @return the dataFields
	 */
	public DataFields getDataFields() {
		return dataFields;
	}

	/**
	 * @param dataFields the dataFields to set
	 */
	public void setDataFields(DataFields dataFields) {
		this.dataFields = dataFields;
	}

	/**
	 * @return the auditFields
	 */
	public AuditFields getAuditFields() {
		return auditFields;
	}

	/**
	 * @param auditFields the auditFields to set
	 */
	public void setAuditFields(AuditFields auditFields) {
		this.auditFields = auditFields;
	}
	
	
}

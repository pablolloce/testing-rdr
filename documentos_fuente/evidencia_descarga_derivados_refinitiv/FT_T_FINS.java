package com.bbva.kytl.refinitivderivativesloader.entities;

import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;



@Entity
@Table (name= "FT_T_FINS", schema="KYTL_GC")
public class FT_T_FINS {

	@Id
	@Column(name = "INST_MNEM")
	private String instMnem;
	
	@Column(name = "CROSS_REF_ID")
	private String crossRefId;
	
	@Column(name = "ORG_ID")
	private String orgId;
	
	@Column(name = "INST_NME")
	private String instNme;
	
	@Column(name = "FISCAL_YR_END_TYP")
	private String fiscalYrEndTyp;
	
	@Column(name = "INST_DESC")
	private String instDesc;
	
	@Column(name = "PREF_FINS_ID_CTXT_TYP")
	private String prefFinsIdCtxtTyp;
	
	@Column(name = "PREF_FINS_ID")
	private String prefFinsId;
	
	@Column(name = "INST_STAT_TYP")
	private String instStatTyp;
	
	@Column(name = "INST_TYP")
	private String instTyp;
	
	@Column(name = "INST_LEGAL_FORM_TYP")
	private String instLegalFormTyp;
	
	@Column(name = "PUBLIC_CORP_IND")
	private String publicCorpInd;
	
	@Column(name = "INST_FOUNDING_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date instFoudingDte;
	
	@Column(name = "BAL_SHEET_CURR_CDE")
	private String balSheetCurrCde;
	
	@Column(name = "DELETE_REAS_TYP")
	private String deleteReasTyp;
	
	@Column(name = "NLS_CDE")
	private String nleCde;
	
	@Column(name = "CMRCL_REGIST_ENTRY_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date cmrclRegistEntryDte;
	
	@Column(name = "CMRCL_REGIST_DELETE_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date cmrclRegistDeleteDte;
	
	@Column(name = "BUSINESS_START_YR_TYP")
	private String businessStartYrTyp;
	
	@Column(name = "IMPORT_EXPORT_AGENT_TYP")
	private String importExportAgentTyp;
	
	@Column(name = "BUSINESS_STRUCTURE_TYP")
	private String businessStructureTyp;
	
	@Column(name = "SUBSIDIARY_IND")
	private String subsidiaryInd;
	
	@Column(name = "MAIL_DLVBLTY_TYP")
	private String mailDlvbltyTyp;
	
	@Column(name = "DUNS_HIER_CDE")
	private Integer dunsHierCde;
	
	@Column(name = "DUNS_DIAS_CDE")
	private String dunsDiasCde;
	
	@Column(name = "DUNS_GLBL_ULT_IND")
	private String dunsGlblUltInd;
	
	@Column(name = "PREF_CURR_CDE")
	private String PrefCurrCde;
	
	@Column(name = "MAIL_ADDR_ID")
	private String mailAddrId;
	
	@Column(name = "ELEC_ADDR_ID")
	private String elecAddrId;
	
	@Column(name = "COMPANY_MATCH_ID")
	private String companyMatchId;
	
	@Column(name = "PREF_ISS_CTXT_TYP")
	private String pressIssCtxtTyp;
	
	@Column(name = "INST_STAT_TMS")
	@Temporal(TemporalType.TIMESTAMP)
	private Date instStatTms;
	
	@Column(name = "INCORPORATION_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date incoporationDte;
	
	@Column(name = "DISSOLUTION_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date dissolutionDte;
	
	@Column(name = "INST_CAT_TYP")
	private String instCatTyp;
	
	@Column(name = "INCORPORATION_PLACE_TXT")
	private String incorporationPlaceTxt;
	
	@Column(name = "INST_LEGAL_NME")
	private String instLegalNme;
	
	@Column(name = "ACQ_BY_PRNT_IND")
	private String acqByPrntInd;
	
	@Column(name = "OBLIGOR_SUBGRP_CLSF_OID")
	private String onligorSubgrpClsfOid;
	
	@Column(name = "OBLIGOR_CL_VALUE")
	private String obligorClValue;
	
	@Column(name = "GOVT_AGENCY_FILING_IND")
	private String govtAgencyFilingInd;
	
	@Column(name = "SEC_FORM_15_IND")
	private String secForm15Ind;
	
	@Column(name = "FUND_SRCE_TYP")
	private String fundSrceTyp;
	
	@Column(name = "LEI_LEGAL_FORM_TXT")
	private String leiLegalFormTxt;
	
	@Column(name = "LEI_NME")
	private String leiNme;
	
	@Column(name = "LEI_RECORD_STAT_TXT")
	private String leiRecordStatTxt;
	
	@Column(name = "ASSOC_INST_MNEM")
	private String assocInstMnem;
	
	@Column(name = "TERMIN_DTE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date terminDte;
	
	@Column(name = "INVEST_FIRM_IND")
	private String investFirmInd;
	
	@Column(name = "HOLDING_COMPANY_IND")
	private String holdingCompanyInd;
	
	@Column(name = "FILF_OID")
	private String filfOid;
	
	@Column(name = "BANKRUPT_IND")
	private String bankruptInd;
	
	@Column(name = "NO_PARENT_IND")
	private String noParentInd;
	
	@Column(name = "SYNTHETIC_ENTITY_IND")
	private String syntheticEntityInd;
	
	@Embedded
	private AuditFields auditFields;
	
	@Embedded
	private DataFields dataFields;

	@OneToMany( fetch = FetchType.LAZY)
	@JoinColumn(name = "INST_MNEM", referencedColumnName = "INST_MNEM", 	nullable = false, insertable = false, updatable = false)
	private Set<FT_T_FINR> finrList;
	
	@OneToMany( fetch = FetchType.LAZY)
	@JoinColumn(name = "INST_MNEM", referencedColumnName = "INST_MNEM", nullable = false, insertable = false, updatable = false)
	private Set<FT_T_FIRL> firlList;
	
	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "INST_MNEM", referencedColumnName = "INST_MNEM", nullable = false, insertable = false, updatable = false)
	private Set<FT_T_FRID> fridList;
	
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
	 * @return the orgId
	 */
	public String getOrgId() {
		return orgId;
	}

	/**
	 * @param orgId the orgId to set
	 */
	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	/**
	 * @return the instNem
	 */
	public String getInstNme() {
		return instNme;
	}

	/**
	 * @param instNem the instNem to set
	 */
	public void setInstNme(String instNme) {
		this.instNme = instNme;
	}

	/**
	 * @return the fiscalYrEndTyp
	 */
	public String getFiscalYrEndTyp() {
		return fiscalYrEndTyp;
	}

	/**
	 * @param fiscalYrEndTyp the fiscalYrEndTyp to set
	 */
	public void setFiscalYrEndTyp(String fiscalYrEndTyp) {
		this.fiscalYrEndTyp = fiscalYrEndTyp;
	}

	/**
	 * @return the instDesc
	 */
	public String getInstDesc() {
		return instDesc;
	}

	/**
	 * @param instDesc the instDesc to set
	 */
	public void setInstDesc(String instDesc) {
		this.instDesc = instDesc;
	}

	/**
	 * @return the prefFinsIdCtxtTyp
	 */
	public String getPrefFinsIdCtxtTyp() {
		return prefFinsIdCtxtTyp;
	}

	/**
	 * @param prefFinsIdCtxtTyp the prefFinsIdCtxtTyp to set
	 */
	public void setPrefFinsIdCtxtTyp(String prefFinsIdCtxtTyp) {
		this.prefFinsIdCtxtTyp = prefFinsIdCtxtTyp;
	}

	/**
	 * @return the prefFinsId
	 */
	public String getPrefFinsId() {
		return prefFinsId;
	}

	/**
	 * @param prefFinsId the prefFinsId to set
	 */
	public void setPrefFinsId(String prefFinsId) {
		this.prefFinsId = prefFinsId;
	}

	/**
	 * @return the instStatTyp
	 */
	public String getInstStatTyp() {
		return instStatTyp;
	}

	/**
	 * @param instStatTyp the instStatTyp to set
	 */
	public void setInstStatTyp(String instStatTyp) {
		this.instStatTyp = instStatTyp;
	}

	/**
	 * @return the instTyp
	 */
	public String getInstTyp() {
		return instTyp;
	}

	/**
	 * @param instTyp the instTyp to set
	 */
	public void setInstTyp(String instTyp) {
		this.instTyp = instTyp;
	}

	/**
	 * @return the instLegalFormTyp
	 */
	public String getInstLegalFormTyp() {
		return instLegalFormTyp;
	}

	/**
	 * @param instLegalFormTyp the instLegalFormTyp to set
	 */
	public void setInstLegalFormTyp(String instLegalFormTyp) {
		this.instLegalFormTyp = instLegalFormTyp;
	}

	/**
	 * @return the publicCorpInd
	 */
	public String getPublicCorpInd() {
		return publicCorpInd;
	}

	/**
	 * @param publicCorpInd the publicCorpInd to set
	 */
	public void setPublicCorpInd(String publicCorpInd) {
		this.publicCorpInd = publicCorpInd;
	}

	/**
	 * @return the instFoudingDte
	 */
	public Date getInstFoudingDte() {
		return instFoudingDte;
	}

	/**
	 * @param instFoudingDte the instFoudingDte to set
	 */
	public void setInstFoudingDte(Date instFoudingDte) {
		this.instFoudingDte = instFoudingDte;
	}

	/**
	 * @return the balSheetCurrCde
	 */
	public String getBalSheetCurrCde() {
		return balSheetCurrCde;
	}

	/**
	 * @param balSheetCurrCde the balSheetCurrCde to set
	 */
	public void setBalSheetCurrCde(String balSheetCurrCde) {
		this.balSheetCurrCde = balSheetCurrCde;
	}

	/**
	 * @return the deleteReasTyp
	 */
	public String getDeleteReasTyp() {
		return deleteReasTyp;
	}

	/**
	 * @param deleteReasTyp the deleteReasTyp to set
	 */
	public void setDeleteReasTyp(String deleteReasTyp) {
		this.deleteReasTyp = deleteReasTyp;
	}

	/**
	 * @return the nleCde
	 */
	public String getNleCde() {
		return nleCde;
	}

	/**
	 * @param nleCde the nleCde to set
	 */
	public void setNleCde(String nleCde) {
		this.nleCde = nleCde;
	}

	/**
	 * @return the cmrclRegistEntryDte
	 */
	public Date getCmrclRegistEntryDte() {
		return cmrclRegistEntryDte;
	}

	/**
	 * @param cmrclRegistEntryDte the cmrclRegistEntryDte to set
	 */
	public void setCmrclRegistEntryDte(Date cmrclRegistEntryDte) {
		this.cmrclRegistEntryDte = cmrclRegistEntryDte;
	}

	/**
	 * @return the cmrclRegistDeleteDte
	 */
	public Date getCmrclRegistDeleteDte() {
		return cmrclRegistDeleteDte;
	}

	/**
	 * @param cmrclRegistDeleteDte the cmrclRegistDeleteDte to set
	 */
	public void setCmrclRegistDeleteDte(Date cmrclRegistDeleteDte) {
		this.cmrclRegistDeleteDte = cmrclRegistDeleteDte;
	}

	/**
	 * @return the businessStartYrTyp
	 */
	public String getBusinessStartYrTyp() {
		return businessStartYrTyp;
	}

	/**
	 * @param businessStartYrTyp the businessStartYrTyp to set
	 */
	public void setBusinessStartYrTyp(String businessStartYrTyp) {
		this.businessStartYrTyp = businessStartYrTyp;
	}

	/**
	 * @return the importExportAgentTyp
	 */
	public String getImportExportAgentTyp() {
		return importExportAgentTyp;
	}

	/**
	 * @param importExportAgentTyp the importExportAgentTyp to set
	 */
	public void setImportExportAgentTyp(String importExportAgentTyp) {
		this.importExportAgentTyp = importExportAgentTyp;
	}

	/**
	 * @return the businessStructureTyp
	 */
	public String getBusinessStructureTyp() {
		return businessStructureTyp;
	}

	/**
	 * @param businessStructureTyp the businessStructureTyp to set
	 */
	public void setBusinessStructureTyp(String businessStructureTyp) {
		this.businessStructureTyp = businessStructureTyp;
	}

	/**
	 * @return the subsidiaryInd
	 */
	public String getSubsidiaryInd() {
		return subsidiaryInd;
	}

	/**
	 * @param subsidiaryInd the subsidiaryInd to set
	 */
	public void setSubsidiaryInd(String subsidiaryInd) {
		this.subsidiaryInd = subsidiaryInd;
	}

	/**
	 * @return the mailDlvbltyTyp
	 */
	public String getMailDlvbltyTyp() {
		return mailDlvbltyTyp;
	}

	/**
	 * @param mailDlvbltyTyp the mailDlvbltyTyp to set
	 */
	public void setMailDlvbltyTyp(String mailDlvbltyTyp) {
		this.mailDlvbltyTyp = mailDlvbltyTyp;
	}

	/**
	 * @return the dunsHierCde
	 */
	public Integer getDunsHierCde() {
		return dunsHierCde;
	}

	/**
	 * @param dunsHierCde the dunsHierCde to set
	 */
	public void setDunsHierCde(Integer dunsHierCde) {
		this.dunsHierCde = dunsHierCde;
	}

	/**
	 * @return the dunsDiasCde
	 */
	public String getDunsDiasCde() {
		return dunsDiasCde;
	}

	/**
	 * @param dunsDiasCde the dunsDiasCde to set
	 */
	public void setDunsDiasCde(String dunsDiasCde) {
		this.dunsDiasCde = dunsDiasCde;
	}

	/**
	 * @return the dunsGlblUltInd
	 */
	public String getDunsGlblUltInd() {
		return dunsGlblUltInd;
	}

	/**
	 * @param dunsGlblUltInd the dunsGlblUltInd to set
	 */
	public void setDunsGlblUltInd(String dunsGlblUltInd) {
		this.dunsGlblUltInd = dunsGlblUltInd;
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
	 * @return the companyMatchId
	 */
	public String getCompanyMatchId() {
		return companyMatchId;
	}

	/**
	 * @param companyMatchId the companyMatchId to set
	 */
	public void setCompanyMatchId(String companyMatchId) {
		this.companyMatchId = companyMatchId;
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
	 * @return the instStatTms
	 */
	public Date getInstStatTms() {
		return instStatTms;
	}

	/**
	 * @param instStatTms the instStatTms to set
	 */
	public void setInstStatTms(Date instStatTms) {
		this.instStatTms = instStatTms;
	}

	/**
	 * @return the incoporationDte
	 */
	public Date getIncoporationDte() {
		return incoporationDte;
	}

	/**
	 * @param incoporationDte the incoporationDte to set
	 */
	public void setIncoporationDte(Date incoporationDte) {
		this.incoporationDte = incoporationDte;
	}

	/**
	 * @return the dissolutionDte
	 */
	public Date getDissolutionDte() {
		return dissolutionDte;
	}

	/**
	 * @param dissolutionDte the dissolutionDte to set
	 */
	public void setDissolutionDte(Date dissolutionDte) {
		this.dissolutionDte = dissolutionDte;
	}

	/**
	 * @return the instCatTyp
	 */
	public String getInstCatTyp() {
		return instCatTyp;
	}

	/**
	 * @param instCatTyp the instCatTyp to set
	 */
	public void setInstCatTyp(String instCatTyp) {
		this.instCatTyp = instCatTyp;
	}

	/**
	 * @return the incorporationPlaceTxt
	 */
	public String getIncorporationPlaceTxt() {
		return incorporationPlaceTxt;
	}

	/**
	 * @param incorporationPlaceTxt the incorporationPlaceTxt to set
	 */
	public void setIncorporationPlaceTxt(String incorporationPlaceTxt) {
		this.incorporationPlaceTxt = incorporationPlaceTxt;
	}

	/**
	 * @return the instLegalNme
	 */
	public String getInstLegalNme() {
		return instLegalNme;
	}

	/**
	 * @param instLegalNme the instLegalNme to set
	 */
	public void setInstLegalNme(String instLegalNme) {
		this.instLegalNme = instLegalNme;
	}

	/**
	 * @return the acqByPrntInd
	 */
	public String getAcqByPrntInd() {
		return acqByPrntInd;
	}

	/**
	 * @param acqByPrntInd the acqByPrntInd to set
	 */
	public void setAcqByPrntInd(String acqByPrntInd) {
		this.acqByPrntInd = acqByPrntInd;
	}

	/**
	 * @return the onligorSubgrpClsfOid
	 */
	public String getOnligorSubgrpClsfOid() {
		return onligorSubgrpClsfOid;
	}

	/**
	 * @param onligorSubgrpClsfOid the onligorSubgrpClsfOid to set
	 */
	public void setOnligorSubgrpClsfOid(String onligorSubgrpClsfOid) {
		this.onligorSubgrpClsfOid = onligorSubgrpClsfOid;
	}

	/**
	 * @return the obligorClValue
	 */
	public String getObligorClValue() {
		return obligorClValue;
	}

	/**
	 * @param obligorClValue the obligorClValue to set
	 */
	public void setObligorClValue(String obligorClValue) {
		this.obligorClValue = obligorClValue;
	}

	/**
	 * @return the govtAgencyFilingInd
	 */
	public String getGovtAgencyFilingInd() {
		return govtAgencyFilingInd;
	}

	/**
	 * @param govtAgencyFilingInd the govtAgencyFilingInd to set
	 */
	public void setGovtAgencyFilingInd(String govtAgencyFilingInd) {
		this.govtAgencyFilingInd = govtAgencyFilingInd;
	}

	/**
	 * @return the secForm15Ind
	 */
	public String getSecForm15Ind() {
		return secForm15Ind;
	}

	/**
	 * @param secForm15Ind the secForm15Ind to set
	 */
	public void setSecForm15Ind(String secForm15Ind) {
		this.secForm15Ind = secForm15Ind;
	}

	/**
	 * @return the fundSrceTyp
	 */
	public String getFundSrceTyp() {
		return fundSrceTyp;
	}

	/**
	 * @param fundSrceTyp the fundSrceTyp to set
	 */
	public void setFundSrceTyp(String fundSrceTyp) {
		this.fundSrceTyp = fundSrceTyp;
	}

	/**
	 * @return the leiLegalFormTxt
	 */
	public String getLeiLegalFormTxt() {
		return leiLegalFormTxt;
	}

	/**
	 * @param leiLegalFormTxt the leiLegalFormTxt to set
	 */
	public void setLeiLegalFormTxt(String leiLegalFormTxt) {
		this.leiLegalFormTxt = leiLegalFormTxt;
	}

	/**
	 * @return the leiNme
	 */
	public String getLeiNme() {
		return leiNme;
	}

	/**
	 * @param leiNme the leiNme to set
	 */
	public void setLeiNme(String leiNme) {
		this.leiNme = leiNme;
	}

	/**
	 * @return the leiRecordStatTxt
	 */
	public String getLeiRecordStatTxt() {
		return leiRecordStatTxt;
	}

	/**
	 * @param leiRecordStatTxt the leiRecordStatTxt to set
	 */
	public void setLeiRecordStatTxt(String leiRecordStatTxt) {
		this.leiRecordStatTxt = leiRecordStatTxt;
	}

	/**
	 * @return the assocInstMnem
	 */
	public String getAssocInstMnem() {
		return assocInstMnem;
	}

	/**
	 * @param assocInstMnem the assocInstMnem to set
	 */
	public void setAssocInstMnem(String assocInstMnem) {
		this.assocInstMnem = assocInstMnem;
	}

	/**
	 * @return the terminDte
	 */
	public Date getTerminDte() {
		return terminDte;
	}

	/**
	 * @param terminDte the terminDte to set
	 */
	public void setTerminDte(Date terminDte) {
		this.terminDte = terminDte;
	}

	/**
	 * @return the investFirmInd
	 */
	public String getInvestFirmInd() {
		return investFirmInd;
	}

	/**
	 * @param investFirmInd the investFirmInd to set
	 */
	public void setInvestFirmInd(String investFirmInd) {
		this.investFirmInd = investFirmInd;
	}

	/**
	 * @return the holdingCompanyInd
	 */
	public String getHoldingCompanyInd() {
		return holdingCompanyInd;
	}

	/**
	 * @param holdingCompanyInd the holdingCompanyInd to set
	 */
	public void setHoldingCompanyInd(String holdingCompanyInd) {
		this.holdingCompanyInd = holdingCompanyInd;
	}

	/**
	 * @return the filfOid
	 */
	public String getFilfOid() {
		return filfOid;
	}

	/**
	 * @param filfOid the filfOid to set
	 */
	public void setFilfOid(String filfOid) {
		this.filfOid = filfOid;
	}

	/**
	 * @return the bankruptInd
	 */
	public String getBankruptInd() {
		return bankruptInd;
	}

	/**
	 * @param bankruptInd the bankruptInd to set
	 */
	public void setBankruptInd(String bankruptInd) {
		this.bankruptInd = bankruptInd;
	}

	/**
	 * @return the noParentInd
	 */
	public String getNoParentInd() {
		return noParentInd;
	}

	/**
	 * @param noParentInd the noParentInd to set
	 */
	public void setNoParentInd(String noParentInd) {
		this.noParentInd = noParentInd;
	}

	/**
	 * @return the syntheticEntityInd
	 */
	public String getSyntheticEntityInd() {
		return syntheticEntityInd;
	}

	/**
	 * @param syntheticEntityInd the syntheticEntityInd to set
	 */
	public void setSyntheticEntityInd(String syntheticEntityInd) {
		this.syntheticEntityInd = syntheticEntityInd;
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
	 * @return the finrList
	 */
	public Set<FT_T_FINR> getFinrList() {
		return finrList;
	}

	/**
	 * @param finrList the finrList to set
	 */
	public void setFinrList(Set<FT_T_FINR> finrList) {
		this.finrList = finrList;
	}

	/**
	 * @return the firlList
	 */
	public Set<FT_T_FIRL> getFirlList() {
		return firlList;
	}

	/**
	 * @param firlList the firlList to set
	 */
	public void setFirlList(Set<FT_T_FIRL> firlList) {
		this.firlList = firlList;
	}

	/**
	 * @return the firdList
	 */
	public Set<FT_T_FRID> getFirdList() {
		return fridList;
	}

	/**
	 * @param firdList the firdList to set
	 */
	public void setFirdList(Set<FT_T_FRID> firdList) {
		this.fridList = firdList;
	}
	
	
	
	
	
}

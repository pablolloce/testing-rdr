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
@Table (name= "FT_T_FIRL", schema="KYTL_GC")
public class FT_T_FIRL {

	@Id
	@Column(name = "FIRL_OID")
	private String firlOid;
	
	@Column(name = "PRNT_INST_MNEM")
	private String prntInstMnem;
	
	@Column(name = "INST_MNEM")
	private String instMnem;
	
	@Column(name = "REL_TYP")
	private String relTyp;
	
	@Column(name = "PART_CURR_CDE")
	private String partCurrCde;
	
	@Column(name = "PART_CAMT")
	private String partCamt;
	
	@Column(name = "PART_CPCT")
	private String partCpct;
	
	@Column(name = "PRIM_REL_IND")
	private String primRelInd;
	
	@Column(name = "REL_STAT_TYP")
	private String relStatTyp;
	
	@Column(name = "REL_STAT_TMS")
	@Temporal(TemporalType.TIMESTAMP)
	private Date relStatTms;
	
	@Column(name = "REL_DESC")
	private String relDesc;
	
	@Column(name = "FINSRL_TYP")
	private String finsrlTyp;
	
	@Column(name = "FINR_OID")
	private String FINR_OID;
	
	@Embedded
	private DataFields dataFields;
	
	@Embedded
	private AuditFields auditFields;

	/**
	 * @return the firlOid
	 */
	public String getFirlOid() {
		return firlOid;
	}

	/**
	 * @param firlOid the firlOid to set
	 */
	public void setFirlOid(String firlOid) {
		this.firlOid = firlOid;
	}

	/**
	 * @return the prntInstMnem
	 */
	public String getPrntInstMnem() {
		return prntInstMnem;
	}

	/**
	 * @param prntInstMnem the prntInstMnem to set
	 */
	public void setPrntInstMnem(String prntInstMnem) {
		this.prntInstMnem = prntInstMnem;
	}

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
	 * @return the relTyp
	 */
	public String getRelTyp() {
		return relTyp;
	}

	/**
	 * @param relTyp the relTyp to set
	 */
	public void setRelTyp(String relTyp) {
		this.relTyp = relTyp;
	}

	/**
	 * @return the partCurrCde
	 */
	public String getPartCurrCde() {
		return partCurrCde;
	}

	/**
	 * @param partCurrCde the partCurrCde to set
	 */
	public void setPartCurrCde(String partCurrCde) {
		this.partCurrCde = partCurrCde;
	}

	/**
	 * @return the partCamt
	 */
	public String getPartCamt() {
		return partCamt;
	}

	/**
	 * @param partCamt the partCamt to set
	 */
	public void setPartCamt(String partCamt) {
		this.partCamt = partCamt;
	}

	/**
	 * @return the partCpct
	 */
	public String getPartCpct() {
		return partCpct;
	}

	/**
	 * @param partCpct the partCpct to set
	 */
	public void setPartCpct(String partCpct) {
		this.partCpct = partCpct;
	}

	/**
	 * @return the primRelInd
	 */
	public String getPrimRelInd() {
		return primRelInd;
	}

	/**
	 * @param primRelInd the primRelInd to set
	 */
	public void setPrimRelInd(String primRelInd) {
		this.primRelInd = primRelInd;
	}

	/**
	 * @return the relStatTyp
	 */
	public String getRelStatTyp() {
		return relStatTyp;
	}

	/**
	 * @param relStatTyp the relStatTyp to set
	 */
	public void setRelStatTyp(String relStatTyp) {
		this.relStatTyp = relStatTyp;
	}

	/**
	 * @return the relStatTms
	 */
	public Date getRelStatTms() {
		return relStatTms;
	}

	/**
	 * @param relStatTms the relStatTms to set
	 */
	public void setRelStatTms(Date relStatTms) {
		this.relStatTms = relStatTms;
	}

	/**
	 * @return the relDesc
	 */
	public String getRelDesc() {
		return relDesc;
	}

	/**
	 * @param relDesc the relDesc to set
	 */
	public void setRelDesc(String relDesc) {
		this.relDesc = relDesc;
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

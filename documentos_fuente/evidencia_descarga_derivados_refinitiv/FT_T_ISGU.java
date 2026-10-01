package com.bbva.kytl.refinitivderivativesloader.entities;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;

import com.bbva.kytl.refinitivderivativesloader.utils.UtilsMethods;

@Entity
@Table (name= "FT_T_ISGU", schema="KYTL_GC")
public class FT_T_ISGU {

	@Id
	@Column(name = "ISGU_OID")
	private String isguOid;
	
	@Column(name = "INSTR_ID")
	private String instrId;
	
	@Column(name = "GU_ID")
	private String guId;
	
	@Column(name = "GU_TYP")
	private String guTyp;
	
	@Column(name = "GU_CNT")
	private Integer guCnt; 
	
	@Column(name = "ISS_GU_PURP_TYP")
	private String issGuPurpTyp;
	
	@Column(name = "GUNT_OID")
	private String guntOid; 
	
	@Embedded
	private AuditFields auditFields;
	
	@Embedded
	private DataFields dataFields;
	

	public DataFields getDataFields() {
		return dataFields;
	}

	public void setDataFields(DataFields dataFields) {
		this.dataFields = dataFields;
	}

	public FT_T_ISGU() {
		
	}
	
	public FT_T_ISGU(EntityManager entityManager) {
		isguOid = UtilsMethods.createOid(entityManager);
	}

	public String getIsguOid() {
		return isguOid;
	}

	public void setIsguOid(String isguOid) {
		this.isguOid = isguOid;
	}

	public String getInstrId() {
		return instrId;
	}

	public void setInstrId(String instrId) {
		this.instrId = instrId;
	}

	public String getGuId() {
		return guId;
	}

	public void setGuId(String guId) {
		this.guId = guId;
	}

	public String getGuTyp() {
		return guTyp;
	}

	public void setGuTyp(String guTyp) {
		this.guTyp = guTyp;
	}

	public Integer getGuCnt() {
		return guCnt;
	}

	public void setGuCnt(Integer guCnt) {
		this.guCnt = guCnt;
	}

	public String getIssGuPurpTyp() {
		return issGuPurpTyp;
	}

	public void setIssGuPurpTyp(String issGuPurpTyp) {
		this.issGuPurpTyp = issGuPurpTyp;
	}

	public String getGuntOid() {
		return guntOid;
	}

	public void setGuntOid(String guntOid) {
		this.guntOid = guntOid;
	}

	public AuditFields getAuditFields() {
		return auditFields;
	}

	public void setAuditFields(AuditFields auditFields) {
		this.auditFields = auditFields;
	}
	
	
}

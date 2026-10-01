package com.bbva.kytl.refinitivderivativesloader.entities;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name= "FT_T_FRID", schema="KYTL_GC")
public class FT_T_FRID {

	
	@Column(name = "FRID_OID")
	@Id
	private String fridOid;
	
	@Column(name = "FINSRL_TYP")
	private String finsrlTyp;
	
	@Column(name = "FINSRL_ID_CTXT_TYP")
	private String finsrlIdCtxtTyp;
	
	@Column(name = "MKT_OID")
	private String mktOid;
	
	@Column(name = "FINR_ID")
	private String finrId;
	
	@Column(name = "GU_ID")
	private String guId;
	
	@Column(name = "GU_TYP")
	private String guTyp;
	
	@Column(name = "GU_CNT")
	private String guCnt;
	
	@Column(name = "MERGE_UNIQ_OID")
	private String mergeUniqOid;
	
	@Column(name = "FINR_CROSS_REF_ID")
	private String finrCrossRefId;
	
	@Column(name = "GUNT_OID")
	private String guntOid;
	
	@Column(name = "FINR_OID")
	private String finrOid;
	
	@Embedded
	private DataFields dataFields;
	
	@Embedded
	private AuditFields auditFields;

	/**
	 * @return the fridOid
	 */
	public String getFridOid() {
		return fridOid;
	}

	/**
	 * @param fridOid the fridOid to set
	 */
	public void setFridOid(String fridOid) {
		this.fridOid = fridOid;
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
	 * @return the finsrlIdCtxtTyp
	 */
	public String getFinsrlIdCtxtTyp() {
		return finsrlIdCtxtTyp;
	}

	/**
	 * @param finsrlIdCtxtTyp the finsrlIdCtxtTyp to set
	 */
	public void setFinsrlIdCtxtTyp(String finsrlIdCtxtTyp) {
		this.finsrlIdCtxtTyp = finsrlIdCtxtTyp;
	}

	/**
	 * @return the mktOid
	 */
	public String getMktOid() {
		return mktOid;
	}

	/**
	 * @param mktOid the mktOid to set
	 */
	public void setMktOid(String mktOid) {
		this.mktOid = mktOid;
	}

	/**
	 * @return the finrId
	 */
	public String getFinrId() {
		return finrId;
	}

	/**
	 * @param finrId the finrId to set
	 */
	public void setFinrId(String finrId) {
		this.finrId = finrId;
	}

	/**
	 * @return the guId
	 */
	public String getGuId() {
		return guId;
	}

	/**
	 * @param guId the guId to set
	 */
	public void setGuId(String guId) {
		this.guId = guId;
	}

	/**
	 * @return the guTyp
	 */
	public String getGuTyp() {
		return guTyp;
	}

	/**
	 * @param guTyp the guTyp to set
	 */
	public void setGuTyp(String guTyp) {
		this.guTyp = guTyp;
	}

	/**
	 * @return the guCnt
	 */
	public String getGuCnt() {
		return guCnt;
	}

	/**
	 * @param guCnt the guCnt to set
	 */
	public void setGuCnt(String guCnt) {
		this.guCnt = guCnt;
	}

	/**
	 * @return the mergeUniqOid
	 */
	public String getMergeUniqOid() {
		return mergeUniqOid;
	}

	/**
	 * @param mergeUniqOid the mergeUniqOid to set
	 */
	public void setMergeUniqOid(String mergeUniqOid) {
		this.mergeUniqOid = mergeUniqOid;
	}

	/**
	 * @return the finrCrossRefId
	 */
	public String getFinrCrossRefId() {
		return finrCrossRefId;
	}

	/**
	 * @param finrCrossRefId the finrCrossRefId to set
	 */
	public void setFinrCrossRefId(String finrCrossRefId) {
		this.finrCrossRefId = finrCrossRefId;
	}

	/**
	 * @return the guntOid
	 */
	public String getGuntOid() {
		return guntOid;
	}

	/**
	 * @param guntOid the guntOid to set
	 */
	public void setGuntOid(String guntOid) {
		this.guntOid = guntOid;
	}

	/**
	 * @return the finrOid
	 */
	public String getFinrOid() {
		return finrOid;
	}

	/**
	 * @param finrOid the finrOid to set
	 */
	public void setFinrOid(String finrOid) {
		this.finrOid = finrOid;
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

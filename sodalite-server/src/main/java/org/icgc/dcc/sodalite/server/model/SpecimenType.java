package org.icgc.dcc.sodalite.server.model;

public enum SpecimenType {
	NORMAL_SOLID_TISSUE("Normal-solid tissue"),
    NORMAL_BLOOD_DERIVED("Normal - blood derived"), 
    NORMAL_BONE_MARROW("Normal - bone marrow"),
    NORMAL_TISSUE_ADJACENT_TO_PRIMARY("Normal - tissue adjacent to primary)"), 
    NORMAL_BUCCAL_CELL("Normal - buccal cell"),
    NORMAL_EBV_IMMORTALIZED("Normal - EBV immortalized"),
    NORMAL_LYMPH_NODE("Normal - lymph node"),
    NORMAL_OTHER("Normal - other"),
    PRIMARY_TUMOR_SOLID_TISSUE("Primary tumour - solid tissue"),
    PRIMARY_TUMOUR_PERIPHERAL_BLOOD("Primary tumour - blood derived (peripheral blood)"),
    PRIMARY_TUMOUR_BONE_MARROW("Primary tumour - blood derived (bone marrow)"),
    PRIMARY_TUMOUR_ADDITONAL_NEW_PRIMARY("Primary tumour - additional new primary"),
    PRIMARY_TUMOUR_OTHER("Primary tumour - other"), 
    RECURRENT_TUMOUR_SOLID_TISSUE("Recurrent tumour - solid tissue"),
    RECURRENT_TUMOUR_PERIPHERAL_BLOOD("Recurrent tumour - blood derived (peripheral blood)"),
    RECURRENT_TUMOUR_BONE_MARROW("Recurrent tumour - blood derived (bone marrow)"),
    RECURRENT_TUMOUR_OTHER("Recurrent tumour - other"),
    METASTATIC_TUMOUR_NOS("Metastatic tumour - NOS"),
    METASTATIC_TUMOUR_LYMPH_NODE("Metastatic tumour - lymph node"),
    METASTATIC_TUMOUR_LOCAL_TO_LYMPH_NODE("Metastatic tumour - metastasis local to lymph node"),
    METASTATIC_TUMOUR_DISTANT_LOCATION("Metastatic tumour - metastasis to distant location"),
    METASTATIC_TUMOUR_ADDTIONAL_METASTATIC("Metastatic tumour - additional metastatic"),
    XENOGRAFT_PRIMARY_TUMOUR("Xenograft - derived from primary tumour"),
    XENOGRAFT_TUMOUR_CELL_LINE("Xenograft - derived from tumour cell line"),
    CELL_LINE_TUMOUR("Cell line - derived from tumour"), 
    PRIMARY_TUMOUR_LYMPH_NODE("Primary tumour - lymph node"),
    METASTATIC_TUMOUR_OTHER("Metastatic tumour - other"), 
    CELL_LINE_XENOGRAFT("Cell line - derived from xenograft tumour");
    
    private final String name;
	SpecimenType(String name) {
		this.name=name;
	}
	public String toString() {
		return name;
	}
}

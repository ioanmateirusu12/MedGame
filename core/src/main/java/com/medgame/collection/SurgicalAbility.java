package com.medgame.collection;

/** Abilities unlocked by discovering specific anatomical structures. */
public enum SurgicalAbility {
    APPENDECTOMY("appendix", "Appendectomy", "Remove the vermiform appendix"),
    VAGOTOMY("vagus_nerve_left", "Vagotomy", "Divide the vagus nerve to reduce acid secretion"),
    VASCULAR_LIGATION("appendicular_artery", "Vascular Ligation",
        "Secure and divide blood vessels with clips"),
    CHOLECYSTECTOMY("gallbladder", "Cholecystectomy", "Laparoscopic removal of the gallbladder"),
    NERVE_BLOCK("femoral_nerve", "Femoral Nerve Block", "Regional anaesthesia of the lower limb"),
    FASCIOTOMY("rectus_abdominis", "Fasciotomy", "Release fascial compartment to reduce pressure"),
    HERNIA_REPAIR("inguinal_ligament", "Hernia Repair", "Repair defect in the abdominal wall"),
    URETEROSCOPY("right_ureter", "Ureteroscopy", "Endoscopic inspection and treatment of the ureter");

    public final String requiredStructureId;
    public final String displayName;
    public final String description;

    SurgicalAbility(String structureId, String displayName, String description) {
        this.requiredStructureId = structureId;
        this.displayName = displayName;
        this.description = description;
    }

    public static SurgicalAbility forStructure(String structureId) {
        for (SurgicalAbility a : values()) {
            if (a.requiredStructureId.equals(structureId)) return a;
        }
        return null;
    }
}

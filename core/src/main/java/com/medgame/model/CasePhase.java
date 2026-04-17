package com.medgame.model;

import java.util.List;

public class CasePhase {

    public String id;
    public String displayName;
    public String phase;        // PREP | INCISION | DISSECTION | PROCEDURE | CLOSURE
    public String requiredRole; // SpecialistRole name
    public List<CaseStep> steps;

    public CasePhase() {}
}

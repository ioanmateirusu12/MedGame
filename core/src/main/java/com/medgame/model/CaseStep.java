package com.medgame.model;

import java.util.List;

public class CaseStep {

    public String id;
    public String instruction;
    public String action;
    public String targetStructure;
    public String toolRequired;
    public String successCondition;
    public String hint;
    public String discoversStructure;
    public List<String> revealStructures;
    public VitalEffect onSuccess;
    public VitalEffect onFailure;
    public boolean canRetry;

    public CaseStep() {}
}

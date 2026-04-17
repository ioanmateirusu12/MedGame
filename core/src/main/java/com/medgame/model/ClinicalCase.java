package com.medgame.model;

import java.util.List;

public class ClinicalCase {

    public String id;
    public String displayName;
    public String description;
    public int difficulty;              // 1-10
    public int estimatedDurationMinutes;
    public int minPlayers;
    public int maxPlayers;
    public String[] requiredRoles;
    public String[] optionalRoles;
    public String region;
    public String surgicalApproach;
    public PatientProfile patientProfile;
    public List<CasePhase> phases;
    public List<String> discoveryOpportunities;
    public CompletionRewards completionRewards;
    public List<String> prerequisites;
    public List<String> nextCases;

    public ClinicalCase() {}

    @Override
    public String toString() {
        return displayName + " [diff:" + difficulty + "]";
    }
}

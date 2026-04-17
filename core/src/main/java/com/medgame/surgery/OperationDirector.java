package com.medgame.surgery;

import com.badlogic.gdx.Gdx;
import com.medgame.model.CasePhase;
import com.medgame.model.CaseStep;
import com.medgame.model.ClinicalCase;
import com.medgame.model.VitalEffect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OperationDirector {

    public static final String TAG = "OperationDirector";

    // Vitals
    public float heartRate;
    public float bloodPressure;
    public float oxygenSat;

    private static final float HEART_RATE_CRITICAL_LOW  = 40f;
    private static final float HEART_RATE_CRITICAL_HIGH = 160f;
    private static final float OXYGEN_SAT_CRITICAL      = 85f;

    // State
    private ClinicalCase clinicalCase;
    private SurgicalPhase currentPhase = SurgicalPhase.PREP;
    private int phaseIndex = 0;
    private int stepIndex  = 0;
    private boolean operationActive = false;
    private int score = 0;
    private long startTime;

    // Discovery tracking
    private final Set<String> discoveredStructures = new HashSet<>();

    // Listeners
    private OperationListener listener;

    public interface OperationListener {
        void onPhaseChanged(SurgicalPhase oldPhase, SurgicalPhase newPhase, CasePhase phaseData);
        void onStepChanged(CaseStep step);
        void onStructureDiscovered(String structureId);
        void onVitalsChanged(float hr, float bp, float o2);
        void onOperationComplete(boolean success, int score, Set<String> discovered);
    }

    public void setListener(OperationListener listener) {
        this.listener = listener;
    }

    public void startOperation(ClinicalCase clinicalCase) {
        this.clinicalCase = clinicalCase;
        this.phaseIndex = 0;
        this.stepIndex  = 0;
        this.score = 0;
        this.discoveredStructures.clear();
        this.operationActive = true;
        this.startTime = System.currentTimeMillis();

        // Init vitals from patient profile
        heartRate     = clinicalCase.patientProfile.baselineHeartRate;
        bloodPressure = 120f; // simplified
        oxygenSat     = clinicalCase.patientProfile.baselineOxygenSat;

        currentPhase = SurgicalPhase.valueOf(getCurrentPhaseData().phase);
        Gdx.app.log(TAG, "Started: " + clinicalCase.displayName);

        if (listener != null) {
            listener.onPhaseChanged(null, currentPhase, getCurrentPhaseData());
            listener.onStepChanged(getCurrentStep());
        }
    }

    /**
     * Called when the local player attempts an action on a structure.
     * Returns true if the action matched the current step and was accepted.
     */
    public boolean submitAction(SurgicalAction action, String structureId,
                                Tool tool, float precision) {
        if (!operationActive) return false;

        CaseStep expected = getCurrentStep();
        if (expected == null) return false;

        boolean actionMatch    = expected.action.equalsIgnoreCase(action.name());
        boolean structureMatch = expected.targetStructure.equalsIgnoreCase(structureId);
        boolean toolMatch      = expected.toolRequired == null
                                 || expected.toolRequired.equalsIgnoreCase(tool.name());

        if (!actionMatch || !structureMatch || !toolMatch) return false;

        float requiredPrecision = parseSuccessCondition(expected.successCondition);
        boolean success = precision >= requiredPrecision;

        if (success) {
            score += 100;
            applyVitalEffect(expected.onSuccess);
            revealStructures(expected.revealStructures);
            if (expected.discoversStructure != null) {
                discover(expected.discoversStructure);
            }
            Gdx.app.log(TAG, "Step PASSED: " + expected.id + " [precision=" + precision + "]");
            advanceStep();
        } else {
            applyVitalEffect(expected.onFailure);
            Gdx.app.log(TAG, "Step FAILED: " + expected.id + " [need=" + requiredPrecision + " got=" + precision + "]");
            if (expected.canRetry) {
                if (listener != null) listener.onVitalsChanged(heartRate, bloodPressure, oxygenSat);
            } else {
                advanceStep(); // skip failed non-retryable step
            }
            checkCritical();
        }
        return success;
    }

    private void advanceStep() {
        CasePhase phase = getCurrentPhaseData();
        stepIndex++;
        if (stepIndex >= phase.steps.size()) {
            advancePhase();
        } else {
            if (listener != null) listener.onStepChanged(getCurrentStep());
        }
    }

    private void advancePhase() {
        phaseIndex++;
        stepIndex = 0;

        if (phaseIndex >= clinicalCase.phases.size()) {
            completeOperation(true);
            return;
        }

        SurgicalPhase oldPhase = currentPhase;
        currentPhase = SurgicalPhase.valueOf(getCurrentPhaseData().phase);
        Gdx.app.log(TAG, "Phase -> " + currentPhase);

        if (listener != null) {
            listener.onPhaseChanged(oldPhase, currentPhase, getCurrentPhaseData());
            listener.onStepChanged(getCurrentStep());
        }
    }

    private void completeOperation(boolean success) {
        operationActive = false;
        currentPhase = success ? SurgicalPhase.COMPLETE : SurgicalPhase.FAILED;
        int finalScore = score + (allOptionalDiscoveriesMade() ? 500 : 0);
        Gdx.app.log(TAG, "Operation " + (success ? "COMPLETE" : "FAILED") + " score=" + finalScore);
        if (listener != null) {
            listener.onOperationComplete(success, finalScore, new HashSet<>(discoveredStructures));
        }
    }

    private void checkCritical() {
        if (heartRate < HEART_RATE_CRITICAL_LOW || heartRate > HEART_RATE_CRITICAL_HIGH
                || oxygenSat < OXYGEN_SAT_CRITICAL) {
            completeOperation(false);
        }
    }

    private void applyVitalEffect(VitalEffect effect) {
        if (effect == null) return;
        heartRate     = Math.max(20f, Math.min(200f, heartRate     + effect.heartRate));
        bloodPressure = Math.max(40f, Math.min(220f, bloodPressure + effect.bloodPressure));
        oxygenSat     = Math.max(60f, Math.min(100f, oxygenSat     + effect.oxygenSat));
        if (listener != null) listener.onVitalsChanged(heartRate, bloodPressure, oxygenSat);
    }

    private void discover(String structureId) {
        if (discoveredStructures.add(structureId)) {
            Gdx.app.log(TAG, "Discovered: " + structureId);
            if (listener != null) listener.onStructureDiscovered(structureId);
        }
    }

    private void revealStructures(List<String> ids) {
        if (ids == null) return;
        for (String id : ids) discover(id);
    }

    private float parseSuccessCondition(String condition) {
        if (condition == null) return 0.5f;
        try {
            // Format: "precision >= 0.65"
            String[] parts = condition.split(">=");
            if (parts.length == 2) return Float.parseFloat(parts[1].trim());
        } catch (Exception ignored) {}
        return 0.5f;
    }

    private boolean allOptionalDiscoveriesMade() {
        if (clinicalCase.discoveryOpportunities == null) return false;
        return discoveredStructures.containsAll(clinicalCase.discoveryOpportunities);
    }

    // --- Getters ---

    public CasePhase getCurrentPhaseData() {
        if (clinicalCase == null || phaseIndex >= clinicalCase.phases.size()) return null;
        return clinicalCase.phases.get(phaseIndex);
    }

    public CaseStep getCurrentStep() {
        CasePhase phase = getCurrentPhaseData();
        if (phase == null || stepIndex >= phase.steps.size()) return null;
        return phase.steps.get(stepIndex);
    }

    public SurgicalPhase getCurrentPhase() { return currentPhase; }

    public int getScore() { return score; }

    public boolean isActive() { return operationActive; }

    public Set<String> getDiscoveredStructures() { return discoveredStructures; }

    public int getTotalSteps() {
        if (clinicalCase == null) return 0;
        int total = 0;
        for (CasePhase p : clinicalCase.phases) total += p.steps.size();
        return total;
    }

    public int getCompletedSteps() {
        if (clinicalCase == null) return 0;
        int completed = 0;
        for (int i = 0; i < phaseIndex; i++) {
            completed += clinicalCase.phases.get(i).steps.size();
        }
        return completed + stepIndex;
    }
}

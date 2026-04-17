package com.medgame.collection;

import com.badlogic.gdx.Gdx;

/**
 * Reacts to new discoveries in CardCollection and logs / broadcasts
 * whenever a surgical ability is unlocked.
 */
public class AbilityUnlockSystem implements CardCollection.DiscoveryListener {

    private static final String TAG = "AbilityUnlock";

    private UnlockListener listener;

    public interface UnlockListener {
        void onAbilityUnlocked(SurgicalAbility ability);
    }

    public AbilityUnlockSystem(CardCollection collection) {
        collection.addListener(this);
    }

    public void setUnlockListener(UnlockListener l) { this.listener = l; }

    @Override
    public void onDiscovered(String structureId, SurgicalAbility ability) {
        if (ability != null) {
            Gdx.app.log(TAG, "Unlocked ability: " + ability.displayName);
            if (listener != null) listener.onAbilityUnlocked(ability);
        }
    }
}

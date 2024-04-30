package com.bayoumi.models.preferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesObservable {
    private final List<PreferencesObserver> observers = new ArrayList<>();

    public void addObserver(PreferencesType key, PreferencesObserver observer) {
        observers.add(new KeyFilteredObserver(key, observer));
    }

    public void removeObserver(PreferencesObserver observer) {
        observers.remove(observer);
    }

    protected void notifyObservers(PreferencesType key, Object value) {
        for (PreferencesObserver observer : observers) {
            if (observer instanceof KeyFilteredObserver) {
                KeyFilteredObserver filteredObserver = (KeyFilteredObserver) observer;
                if (filteredObserver.getKey() == key) {
                    observer.update(key, value);
                }
            }
        }
    }

    private static class KeyFilteredObserver implements PreferencesObserver {
        private final PreferencesType key;
        private final PreferencesObserver observer;

        public KeyFilteredObserver(PreferencesType key, PreferencesObserver observer) {
            this.key = key;
            this.observer = observer;
        }

        public PreferencesType getKey() {
            return key;
        }

        @Override
        public void update(PreferencesType key, Object value) {
            observer.update(key, value);
        }
    }
}

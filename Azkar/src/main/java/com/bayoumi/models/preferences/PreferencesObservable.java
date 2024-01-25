package com.bayoumi.models.preferences;

import java.util.ArrayList;
import java.util.List;

public class PreferencesObservable {
    private final List<PreferencesObserver> OBSERVERS = new ArrayList<>();

    public void addObserver(PreferencesObserver observer) {
        OBSERVERS.add(observer);
    }

    public void removeObserver(PreferencesObserver observer) {
        OBSERVERS.remove(observer);
    }

    protected void notifyObservers(PreferencesType key, String value) {
        for (PreferencesObserver observer : OBSERVERS) {
            observer.update(key, value);
        }
    }

    /*
    TODO: create function to add observer with a filter to notify only if the key is matched
      package com.bayoumi.models.preferences;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

        public class PreferencesObservable {
            private final Map<PreferencesType, List<PreferencesObserver>> OBSERVERS = new HashMap<>();

            public void addObserver(PreferencesType type, PreferencesObserver observer) {
                OBSERVERS.computeIfAbsent(type, k -> new ArrayList<>()).add(observer);
            }

            public void removeObserver(PreferencesType type, PreferencesObserver observer) {
                List<PreferencesObserver> observers = OBSERVERS.get(type);
                if (observers != null) {
                    observers.remove(observer);
                }
            }

            protected void notifyObservers(PreferencesType key, String value) {
                if (key == null) {
                    for (List<PreferencesObserver> observers : OBSERVERS.values()) {
                        for (PreferencesObserver observer : observers) {
                            observer.update(null, value);
                        }
                    }
                } else {
                    List<PreferencesObserver> observers = OBSERVERS.get(key);
                    if (observers != null) {
                        for (PreferencesObserver observer : observers) {
                            observer.update(key, value);
                        }
                    }
                }
            }
        }
    * */
}

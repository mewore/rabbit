const SETTINGS_KEY = 'rabbit-reisen:settings';

const initialSettingsRaw =
    (window.localStorage && window.localStorage.getItem(SETTINGS_KEY)) ||
    (window.sessionStorage && window.sessionStorage.getItem(SETTINGS_KEY));
const initialSettings = initialSettingsRaw ? JSON.parse(initialSettingsRaw) : {};

export enum SaveLocation {
    NOWHERE,
    SESSION,
    STORAGE,
}

export interface Settings {
    saveTo: SaveLocation;
    quality: number;
    shadows: boolean;
    plantsReceiveShadows: boolean;
    plantVisibility: number;
    darkUi: boolean;
    forestWallActiveRadius: number;
    showPerformance: boolean;
    debugPhysics: boolean;
    artificialLatency: number;
}

let currentSettings: Settings = {
    saveTo: SaveLocation.NOWHERE,
    quality: 1.0,
    shadows: true,
    plantsReceiveShadows: false,
    plantVisibility: 0.5,
    darkUi: true,
    forestWallActiveRadius: 200,
    showPerformance: false,
    debugPhysics: false,
    artificialLatency: 0,
    ...initialSettings,
};

export function getSettings(): Settings {
    return JSON.parse(JSON.stringify(currentSettings));
}

export function setSettings(newSettings: Settings): void {
    if (currentSettings.saveTo !== newSettings.saveTo) {
        switch (currentSettings.saveTo) {
            case SaveLocation.SESSION:
                sessionStorage.removeItem(SETTINGS_KEY);
                break;
            case SaveLocation.STORAGE:
                localStorage.removeItem(SETTINGS_KEY);
                break;
        }
    }
    currentSettings = JSON.parse(JSON.stringify(newSettings));
    switch (currentSettings.saveTo) {
        case SaveLocation.SESSION:
            sessionStorage.setItem(SETTINGS_KEY, JSON.stringify(currentSettings));
            break;
        case SaveLocation.STORAGE:
            localStorage.setItem(SETTINGS_KEY, JSON.stringify(currentSettings));
            break;
    }
}

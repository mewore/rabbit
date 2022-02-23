// Temporary utility functions which should be converted into proper code

export enum SiteType {
    REISEN,
    TEWI,
    UNKNOWN,
}

export function getSiteType(): SiteType {
    const match = window.location.href.match(/^http:\/\/(www\.)?([a-z.]+)[/:]/);
    if (!match || !match[2]) {
        return SiteType.UNKNOWN;
    }

    switch (match[2]) {
        case 'localhost':
        case 'rabbit.reisen':
        case 'frankfurt.mewore.moe':
            return SiteType.REISEN;
        case 'tewi.reisen':
            return SiteType.TEWI;
        default:
            return SiteType.UNKNOWN;
    }
}

export interface LinkInfo {
    text: string;
    url?: string;
}

export interface Credit {
    thing: LinkInfo;
    author: LinkInfo;
}
const zunLink: LinkInfo = { text: 'ZUN', url: 'https://en.touhouwiki.net/wiki/ZUN' };

const credits: Credit[] = [
    {
        thing: { text: 'Vue.js', url: 'https://vuejs.org/' },
        author: { text: 'Evan You', url: 'https://evanyou.me/' },
    },
    {
        thing: { text: 'Quasar framework', url: 'https://quasar.dev/' },
        author: { text: 'Razvan Stoenescu', url: 'https://github.com/rstoenescu' },
    },
    {
        thing: { text: 'Reisen', url: 'https://en.touhouwiki.net/wiki/Reisen_Udongein_Inaba' },
        author: zunLink,
    },
];

export function getCredits(): ReadonlyArray<Credit> {
    return credits;
}

export function addCredit(newCredit: Credit): void {
    if (credits.some((credit) => credit.thing.text === newCredit.thing.text)) {
        return;
    }
    credits.push(newCredit);
}

if (getSiteType() === SiteType.TEWI) {
    addCredit({ thing: { text: 'Tewi', url: 'https://en.touhouwiki.net/wiki/Tewi_Inaba' }, author: zunLink });
}

export function isReisen(): boolean {
    return getSiteType() === SiteType.REISEN;
}

export function getTitle(): string {
    switch (getSiteType()) {
        case SiteType.REISEN:
            return 'Reisen';
        case SiteType.TEWI:
            return "Tewi (actually just a carrot because I don't have a model for her :c)";
    }
    return '';
}

const SETTINGS_KEY = 'rabbit-reisen:settings';

const initialSettingsRaw = window.localStorage?.getItem(SETTINGS_KEY) || window.sessionStorage?.getItem(SETTINGS_KEY);
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

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
    url: string;
}

export interface Credit {
    thing: LinkInfo;
    author: LinkInfo;
}
const zunLink: LinkInfo = { text: 'ZUN', url: 'https://en.touhouwiki.net/wiki/ZUN' };

const credits: Credit[] = [
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

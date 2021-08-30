// Temporary utility functions which should be converted into proper code

export function addCredit(html: string): void {
    const newElement = document.createElement('div');
    newElement.innerHTML = html;
    document.getElementById('footer')?.appendChild(newElement);
}

export function isReisen(): boolean {
    return getSiteType() === SiteType.REISEN;
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

export enum SiteType {
    REISEN,
    TEWI,
    UNKNOWN,
}

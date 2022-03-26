export interface FrameInfo {
    frameId: number;
    imageData: string;
    messages: FrameMessage[];
    worldUpdateState?: WorldUpdateState;
}

export enum WorldUpdateState {
    ACCEPTED,
    REJECTED,
}

export interface FrameMessage {
    text: string;
    attachments?: FrameMessageAttachment[];
}

export interface FrameMessageAttachment {
    reference: object;
    icon: string;
    tooltip: string;
}

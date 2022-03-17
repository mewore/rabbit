export interface FrameInfo {
    frameId: number;
    imageData: string;
    messages: FrameMessage[];
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

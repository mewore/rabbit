import { FrameDataType } from './frame-data-type';
import { FrameSection } from './frame-section';

export class FrameCompiler {
    private size = 0;

    reserve(...elementTypes: FrameDataType[]): FrameSection {
        const reservationSize = elementTypes.map((type) => type.byteSize).reduce((prev, cur) => prev + cur, 0);
        const result = new FrameSection(this.size, reservationSize);
        this.size += reservationSize;
        return result;
    }

    public reserveMultiple(count: number, ...elementTypes: FrameDataType[]): FrameSection[] {
        const result: FrameSection[] = [];
        for (let i = 0; i < count; i++) {
            result.push(this.reserve(...elementTypes));
        }
        return result;
    }
}

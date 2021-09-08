export class Input {
    active = true;

    mouseSensitivity = Math.PI / 1000.0;
    mouseZoomSensitivity = 1 / 128;

    private up = false;
    private down = false;
    private left = false;
    private right = false;
    private mouseMovementY = 0;
    private mouseMovementX = 0;
    private mouseScrollUp = 0;

    get movementRight(): number {
        return this.active ? (this.right ? 1 : 0) - (this.left ? 1 : 0) : 0;
    }

    get movementForwards(): number {
        return this.active ? (this.down ? 1 : 0) - (this.up ? 1 : 0) : 0;
    }

    /**
     * How much to look right, in radians. Negative if looking left.
     */
    get lookRight(): number {
        return this.active ? this.mouseMovementX * this.mouseSensitivity : 0;
    }

    /**
     * How much to look down, in radians. Negative if looking up.
     */
    get lookDown(): number {
        return this.active ? this.mouseMovementY * this.mouseSensitivity : 0;
    }

    get zoom(): number {
        return this.mouseScrollUp * this.mouseZoomSensitivity;
    }

    clear(): void {
        this.up = this.down = this.left = this.right = false;
        this.clearMouseDelta();
    }

    clearMouseDelta(): void {
        this.mouseMovementX = this.mouseMovementY = this.mouseScrollUp = 0;
    }

    processMouseMovement(movementX: number, movementY: number): void {
        this.mouseMovementX += movementX;
        this.mouseMovementY += movementY;
    }

    processMouseWheel(wheelDelta: number): void {
        this.mouseScrollUp += wheelDelta;
    }

    processKey(keyCode: string, isDown: boolean): void {
        switch (keyCode) {
            case 'KeyW':
            case 'ArrowUp':
                this.up = isDown;
                break;
            case 'KeyA':
            case 'ArrowLeft':
                this.left = isDown;
                break;
            case 'KeyS':
            case 'ArrowDown':
                this.down = isDown;
                break;
            case 'KeyD':
            case 'ArrowRight':
                this.right = isDown;
                break;
        }
    }
}

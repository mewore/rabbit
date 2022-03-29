export class Input {
    id = -1;
    active = true;

    mouseSensitivity = Math.PI / 1000.0;
    mouseZoomSensitivity = 1 / 128;

    private readonly rotationRequiredForNewInput = Math.PI / 25;

    private mouseX = 0;
    private mouseXSinceLastInput = -Math.PI;

    private up = false;
    private down = false;
    private left = false;
    private right = false;
    private jump = false;
    private mouseMovementY = 0;
    private mouseMovementX = 0;
    private mouseScrollUp = 0;

    get movementRight(): number {
        return (this.right ? 1 : 0) - (this.left ? 1 : 0);
    }

    get movementForwards(): number {
        return (this.down ? 1 : 0) - (this.up ? 1 : 0);
    }

    get isUpPressed(): boolean {
        return this.up;
    }

    get isDownPressed(): boolean {
        return this.down;
    }

    get isLeftPressed(): boolean {
        return this.left;
    }

    get isRightPressed(): boolean {
        return this.right;
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

    get wantsToJump(): boolean {
        return this.active && this.jump;
    }

    get zoom(): number {
        return this.mouseScrollUp * this.mouseZoomSensitivity;
    }

    get hasMovement(): boolean {
        return Math.abs(this.movementRight) + Math.abs(this.movementForwards) > 0.001;
    }

    clear(): void {
        if (this.hasMovement) {
            this.id++;
            this.mouseXSinceLastInput = -this.mouseX;
        }
        this.up = this.down = this.left = this.right = this.jump = false;
        this.clearMouseDelta();
    }

    clearMouseDelta(): void {
        this.mouseMovementX = this.mouseMovementY = this.mouseScrollUp = 0;
    }

    processMouseMovement(movementX: number, movementY: number): void {
        this.mouseMovementX += movementX;
        this.mouseMovementY += movementY;
        this.mouseX += movementX * this.mouseSensitivity;
        if (this.hasMovement && Math.abs(this.mouseX - this.mouseXSinceLastInput) > this.rotationRequiredForNewInput) {
            this.id++;
            this.mouseXSinceLastInput = this.mouseX;
        }
    }

    processMouseWheel(wheelDelta: number): void {
        this.mouseScrollUp += wheelDelta;
    }

    processKey(keyCode: string, isDown: boolean): void {
        switch (keyCode) {
            case 'KeyW':
            case 'ArrowUp':
                if (this.up !== isDown) {
                    this.id++;
                    this.mouseXSinceLastInput = this.mouseX;
                    this.up = isDown;
                }
                break;
            case 'KeyA':
            case 'ArrowLeft':
                if (this.left !== isDown) {
                    this.id++;
                    this.mouseXSinceLastInput = this.mouseX;
                    this.left = isDown;
                }
                break;
            case 'KeyS':
            case 'ArrowDown':
                if (this.down !== isDown) {
                    this.id++;
                    this.mouseXSinceLastInput = this.mouseX;
                    this.down = isDown;
                }
                break;
            case 'KeyD':
            case 'ArrowRight':
                if (this.right !== isDown) {
                    this.id++;
                    this.mouseXSinceLastInput = this.mouseX;
                    this.right = isDown;
                }
                break;
            case 'Space':
                if (this.jump !== isDown) {
                    this.id++;
                    this.mouseXSinceLastInput = this.mouseX;
                    this.jump = isDown;
                }
                break;
        }
    }
}

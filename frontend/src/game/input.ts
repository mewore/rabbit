export class Input {
    active = true;
    up = false;
    down = false;
    left = false;
    right = false;

    get side(): number {
        return this.active ? (this.right ? 1 : 0) - (this.left ? 1 : 0) : 0;
    }

    get forwards(): number {
        return this.active ? (this.down ? 1 : 0) - (this.up ? 1 : 0) : 0;
    }

    clear(): void {
        this.up = this.down = this.left = this.right = false;
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

export class LazyLoadAllocation {
    private allocated = 0;

    /**
     * Use resources if that many are available.
     *
     * @param amount The resource amount to use.
     * @returns Whether there were enough resources.
     */
    tryToUse(amount: number): boolean {
        if (this.allocated < amount) {
            return false;
        }
        window.console.log(`${this.allocated} -> ${this.allocated - amount}`);
        this.allocated -= amount;
        return true;
    }

    /**
     * Use some resources. If the available resources are less than the amount, go into "debt" - negative resources.
     *
     * @param amount The resource amount to use.
     */
    useRetroactively(amount: number): void {
        this.allocated -= amount;
    }

    /**
     * Receive some resources.
     *
     * @param limit The maximum allocated resources after this operation.
     * @param maxToAllocate The maximum resources to receive. Same as [limit] by default.
     * @returns How much has been allocated.
     */
    allocateUpTo(limit: number, maxToAllocate = limit): number {
        const newAllocated = Math.min(limit - this.allocated, maxToAllocate);
        this.allocated += newAllocated;
        return newAllocated;
    }
}

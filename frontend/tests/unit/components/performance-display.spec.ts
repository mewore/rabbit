import { describe, expect, it } from '@jest/globals';
import { shallowMount } from '@vue/test-utils';

import PerformanceDisplay from '@/components/PerformanceDisplay.vue';

describe('PerformanceDisplay.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(PerformanceDisplay);
        expect(wrapper.text()).toContain('FPS:');
    });
});

import { shallowMount } from '@vue/test-utils';
import { expect } from 'chai';

import PerformanceDisplay from '@/components/PerformanceDisplay.vue';

describe('PerformanceDisplay.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(PerformanceDisplay);
        expect(wrapper.text()).to.include('FPS:');
    });
});

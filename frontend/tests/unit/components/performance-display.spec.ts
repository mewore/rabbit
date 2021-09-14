import PerformanceDisplay from '@/components/PerformanceDisplay.vue';
import { expect } from 'chai';
import { shallowMount } from '@vue/test-utils';

describe('PerformanceDisplay.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(PerformanceDisplay);
        expect(wrapper.text()).to.include('FPS:');
    });
});

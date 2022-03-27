import { describe, expect, it } from '@jest/globals';
import { shallowMount } from '@vue/test-utils';

import Credits from '@/components/menu/Credits.vue';

describe('Credits.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(Credits);
        expect(wrapper.text()).toContain('Reisen');
        expect(wrapper.text()).toContain('ZUN');

        // I'm sorry, Tewi
        expect(wrapper.text()).not.toContain('Tewi');
    });
});

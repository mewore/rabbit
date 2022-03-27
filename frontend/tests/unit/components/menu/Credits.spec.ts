import { describe, expect, it } from '@jest/globals';
import { shallowMount } from '@vue/test-utils';

import CreditMenu from '@/components/menu/CreditMenu.vue';

describe('CreditMenu.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(CreditMenu);
        expect(wrapper.text()).toContain('Reisen');
        expect(wrapper.text()).toContain('ZUN');

        // I'm sorry, Tewi
        expect(wrapper.text()).not.toContain('Tewi');
    });
});

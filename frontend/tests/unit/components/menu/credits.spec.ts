import { shallowMount } from '@vue/test-utils';
import { expect } from 'chai';

import Credits from '@/components/menu/Credits.vue';

describe('Credits.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(Credits);
        expect(wrapper.text()).to.include('Reisen');
        expect(wrapper.text()).to.include('ZUN');

        // I'm sorry, Tewi
        expect(wrapper.text()).not.to.include('Tewi');
    });
});

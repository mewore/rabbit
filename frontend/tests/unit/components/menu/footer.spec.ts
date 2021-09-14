import Footer from '@/components/menu/Footer.vue';
import { expect } from 'chai';
import { shallowMount } from '@vue/test-utils';

describe('Footer.vue', () => {
    it('should be rendered properly', () => {
        const wrapper = shallowMount(Footer);
        expect(wrapper.text()).to.include('Reisen');
        expect(wrapper.text()).to.include('ZUN');

        // I'm sorry, Tewi
        expect(wrapper.text()).not.to.include('Tewi');
    });
});

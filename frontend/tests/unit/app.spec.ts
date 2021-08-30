import App from '@/App.vue';
import { expect } from 'chai';
import { shallowMount } from '@vue/test-utils';

describe('App.vue', () => {
    it('renders props.msg when passed', () => {
        const wrapper = shallowMount(App);
        expect(wrapper.text()).to.include('Reisen');
        expect(wrapper.text()).to.include('ZUN');

        // I'm sorry, Tewi
        expect(wrapper.text()).not.to.include('Tewi');
    });
});

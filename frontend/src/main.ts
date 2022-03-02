import 'mosha-vue-toastify/dist/style.css';

import { Quasar } from 'quasar';
import { createApp } from 'vue';

import App from './App.vue';
import quasarUserOptions from './quasar-user-options';
import { getTitle } from './temp-util';

document.title = getTitle();
createApp(App).use(Quasar, quasarUserOptions).mount('#app');

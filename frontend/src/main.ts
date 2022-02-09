import 'mosha-vue-toastify/dist/style.css';
import App from './App.vue';
import { Quasar } from 'quasar';
import { createApp } from 'vue';
import { getTitle } from './temp-util';
import quasarUserOptions from './quasar-user-options';

document.title = getTitle();
createApp(App).use(Quasar, quasarUserOptions).mount('#app');

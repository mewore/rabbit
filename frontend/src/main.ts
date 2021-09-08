import 'mosha-vue-toastify/dist/style.css';
import App from './App.vue';
import { createApp } from 'vue';
import { getTitle } from './temp-util';

document.title = getTitle();
createApp(App).mount('#app');

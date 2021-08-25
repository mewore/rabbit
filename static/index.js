document.addEventListener('readystatechange', (event) => {
    if (document.readyState !== 'interactive') {
        return;
    }
    console.log('Ready!');
    const title = getTitle();
    if (!title) {
        return;
    }
    document.title = title;
    document.getElementById('title-header').innerText = title;
});

function getTitle() {
    const match = window.location.href.match(/^http:\/\/(www\.)?([a-z\.]+)[\/:]/);
    if (!match || !match[2]) {
        return undefined;
    }
    switch (match[2]) {
        case 'localhost':
        case 'rabbit.reisen':
        case 'frankfurt.mewore.moe':
            return 'Reisen';
        case 'tewi.reisen':
            return "Tewi (actually just a carrot because I don't have a model for her :c)";
        default:
            return undefined;
    }
}

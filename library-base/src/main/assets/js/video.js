(function() {
    var video = document.querySelector('video');
    if (!video) return;
    var timer;
    video.addEventListener('touchstart', function() {
        timer = setInterval(function() {
            video.currentTime += 5;
        }, 1000);
    });
    video.addEventListener('touchcancel', function() {
        clearInterval(timer);
    });
    video.addEventListener('touchend', function() {
        clearInterval(timer);
    });
})();
(function() {
    window.quickBack5 = function() {
        var videos = document.querySelectorAll('video');
        var playingVideos = [];
        videos.forEach(function(video) {
            if (!video.paused && !video.ended && video.readyState > 2) {
                playingVideos.push(video);
            }
        });
        var video = playingVideos.pop()
        if (!video) return;
        video.currentTime -= 5;
        console.log('Current time after quick back 5 seconds:', video.currentTime);
    }
    window.quickBack10 = function() {
        var videos = document.querySelectorAll('video');
        var playingVideos = [];
        videos.forEach(function(video) {
            if (!video.paused && !video.ended && video.readyState > 2) {
                playingVideos.push(video);
            }
        });
        var video = playingVideos.pop()
        if (!video) return;
        video.currentTime -= 10;
        console.log('Current time after quick back 10 seconds:', video.currentTime);
    }
    window.quickForward5 = function() {
        var videos = document.querySelectorAll('video');
        var playingVideos = [];
        videos.forEach(function(video) {
            if (!video.paused && !video.ended && video.readyState > 2) {
                playingVideos.push(video);
            }
        });
        var video = playingVideos.pop()
        if (!video) return;
        video.currentTime += 5;
        console.log('Current time after quick forward 5 seconds:', video.currentTime);
    }
    window.quickForward10 = function() {
        var videos = document.querySelectorAll('video');
        var playingVideos = [];
        videos.forEach(function(video) {
            if (!video.paused && !video.ended && video.readyState > 2) {
                playingVideos.push(video);
            }
        });
        var video = playingVideos.pop()
        if (!video) return;
        video.currentTime += 10;
        console.log('Current time after quick forward 10 seconds:', video.currentTime);
    }
})()